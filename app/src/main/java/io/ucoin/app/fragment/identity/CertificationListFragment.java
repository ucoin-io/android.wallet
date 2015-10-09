package io.ucoin.app.fragment.identity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListFragment;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.android.volley.NoConnectionError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import java.util.HashMap;
import java.util.Map;

import io.ucoin.app.Application;
import io.ucoin.app.R;
import io.ucoin.app.activity.LookupActivity;
import io.ucoin.app.adapter.CertificationSectionCursorAdapter;
import io.ucoin.app.content.DbProvider;
import io.ucoin.app.enumeration.CertificationType;
import io.ucoin.app.model.UcoinBlock;
import io.ucoin.app.model.UcoinEndpoint;
import io.ucoin.app.model.UcoinIdentity;
import io.ucoin.app.model.UcoinMember;
import io.ucoin.app.model.document.Certification;
import io.ucoin.app.model.document.SelfCertification;
import io.ucoin.app.model.http_api.WotLookup;
import io.ucoin.app.model.sql.sqlite.Identity;
import io.ucoin.app.sqlite.SQLiteTable;
import io.ucoin.app.technical.crypto.AddressFormatException;


public class CertificationListFragment extends ListFragment
        implements LoaderManager.LoaderCallbacks<Cursor>,
        Response.ErrorListener, Response.Listener<String> {

    private final static String CURRENCY_ID = "currency_id";
    private final static String IDENTITY_ID = "identity_id";

    static public CertificationListFragment newInstance(Long currencyId, Long identityId, CertificationType type) {
        Bundle newInstanceArgs = new Bundle();
        newInstanceArgs.putLong(CURRENCY_ID, currencyId);
        newInstanceArgs.putLong(IDENTITY_ID, identityId);
        newInstanceArgs.putSerializable(CertificationType.class.getSimpleName(), type);
        CertificationListFragment fragment = new CertificationListFragment();
        fragment.setArguments(newInstanceArgs);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        return inflater.inflate(R.layout.fragment_certification_list,
                container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        CertificationSectionCursorAdapter certificationSectionCursorAdapter
                = new CertificationSectionCursorAdapter(getActivity(), null, 0);
        setListAdapter(certificationSectionCursorAdapter);
        getLoaderManager().initLoader(0, getArguments(), this);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        CertificationType type =
                (CertificationType) getArguments().getSerializable(CertificationType.class.getSimpleName());
        if (type == CertificationType.BY) {
            inflater.inflate(R.menu.toolbar_certified, menu);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_certify:
                Intent intent = new Intent(getActivity(),
                        LookupActivity.class);
                intent.putExtra(LookupActivity.CURRENCY_ID, getArguments().getLong(CURRENCY_ID));
                startActivityForResult(intent, Application.ACTIVITY_LOOKUP);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (resultCode != Activity.RESULT_OK)
            return;

        if (requestCode == Application.ACTIVITY_LOOKUP) {
            showConfirmDialog(intent.getExtras());
        }
    }


    public void showConfirmDialog(Bundle args) {
        final WotLookup.Result result = (WotLookup.Result) args.getSerializable(WotLookup.Result.class.getSimpleName());
        String message = getResources().getString(R.string.do_you_want_to_certify_this_uid);
        message += "\n" + result.uids[0].uid;
        message += "\n" + result.pubkey;
        message += "\n" + result.uids[0].self;
        message += "\n" + result.uids[0].meta.timestamp;
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.confirm_certification)
                .setMessage(message);

        builder.setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                final UcoinIdentity identity = new Identity(getActivity(), getArguments().getLong(IDENTITY_ID));
                final UcoinMember member;
                if (identity.members().getBySelf(result.uids[0].self) == null) {
                    member = identity.members().add(result);
                    if(member == null) return;
                } else {
                    member = identity.members().getBySelf(result.uids[0].self);
                }

                final SelfCertification selfCertification = new SelfCertification();
                selfCertification.uid = member.uid();
                selfCertification.timestamp = member.timestamp();
                selfCertification.signature = member.self();

                UcoinBlock currentBlock = identity.currency().blocks().currentBlock();

                final Certification certification = new Certification();
                certification.selfCertification = selfCertification;
                certification.blockNumber = currentBlock.number();
                certification.blockHash = currentBlock.hash();

                certification.certifierPublicKey = identity.wallet().publicKey();
                certification.certifiedPublicKey = member.publicKey();

                try {
                    certification.certifierSignature = certification.sign(identity.wallet().privateKey());
                } catch (AddressFormatException e) {
                    Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_LONG).show();
                }

                UcoinEndpoint endpoint = identity.currency().peers().at(0).endpoints().at(0);
                String url = "http://" + endpoint.ipv4() + ":" + endpoint.port() + "/wot/add/";

                StringRequest request = new StringRequest(
                        Request.Method.POST,
                        url,
                        CertificationListFragment.this,
                        CertificationListFragment.this) {
                    @Override
                    protected Map<String, String> getParams() {
                        Map<String, String> params = new HashMap<String, String>();
                        params.put("pubkey", member.publicKey());
                        params.put("self", selfCertification.toString());
                        params.put("other", certification.inline());
                        return params;
                    }
                };
                request.setTag(this);
                Application.getRequestQueue().add(request);
            }
        });

        builder.setNegativeButton(R.string.CANCEL, null);

        AlertDialog dialog = builder.create();
        dialog.show();
    }


    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {

        Long identityId = args.getLong(IDENTITY_ID);
        CertificationType type = (CertificationType) args.getSerializable(CertificationType.class.getSimpleName());

        String selection = SQLiteTable.Certification.IDENTITY_ID + "=? AND " + SQLiteTable.Certification.TYPE + "=?";
        String selectionArgs[] = new String[]{
                identityId.toString(),
                type.name()
        };

        return new CursorLoader(
                getActivity(),
                DbProvider.CERTIFICATION_URI,
                null, selection, selectionArgs,
                SQLiteTable.Certification.BLOCK + " DESC");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        ((CertificationSectionCursorAdapter) this.getListAdapter()).swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        ((CertificationSectionCursorAdapter) this.getListAdapter()).swapCursor(null);
    }

    @Override
    public void onErrorResponse(VolleyError error) {

        if (error instanceof NoConnectionError) {
            Toast.makeText(Application.getContext(),
                    getResources().getString(R.string.no_connection),
                    Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(Application.getContext(), error.toString(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onResponse(String response) {
        Toast.makeText(getActivity(), getResources().getString(R.string.certification_sent), Toast.LENGTH_LONG).show();
        Application.requestSync();
    }
}