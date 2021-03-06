package io.ucoin.app.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NoConnectionError;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import io.ucoin.app.Application;
import io.ucoin.app.R;
import io.ucoin.app.adapter.LookupAdapter;
import io.ucoin.app.model.UcoinCurrency;
import io.ucoin.app.model.UcoinEndpoint;
import io.ucoin.app.model.http_api.WotLookup;
import io.ucoin.app.model.sql.sqlite.Currency;

public class LookupActivity extends ActionBarActivity implements SearchView.OnQueryTextListener,
        ListView.OnItemClickListener {

    private Toolbar mToolbar;
    private ProgressBar mProgressBar;
    private ListView mListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_lookup);

        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        mProgressBar = (ProgressBar) findViewById(R.id.progress_bar);
        mListView = (ListView) findViewById(R.id.listview);
        mListView.setEmptyView(findViewById(R.id.empty));
        mListView.setOnItemClickListener(this);
        LookupAdapter adapter = new LookupAdapter(this);
        mListView.setAdapter(adapter);
        try {
            setSupportActionBar(mToolbar);
        } catch (Throwable t) {
            Log.w("setSupportActionBar", t.getMessage());
        }
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        mToolbar.inflateMenu(R.menu.toolbar_lookup);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem searchViewItem = menu.findItem(R.id.action_lookup);
        SearchView searchView = (SearchView) searchViewItem.getActionView();
        searchView.setIconifiedByDefault(false);
        searchView.requestFocus();
        searchView.setOnQueryTextListener(this);
        ((InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE)).
                toggleSoftInput(InputMethodManager.SHOW_FORCED,
                        InputMethodManager.HIDE_IMPLICIT_ONLY);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                super.onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onStop() {
        super.onStop();
        Application.getRequestQueue().cancelAll(this);
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        Application.getRequestQueue().cancelAll(this);
        query = query.trim();

        mProgressBar.setVisibility(View.VISIBLE);
        mListView.setVisibility(View.GONE);
        UcoinCurrency currency = new Currency(this, getIntent().getExtras().getLong(Application.EXTRA_CURRENCY_ID));
        UcoinEndpoint endpoint = currency.peers().at(0).endpoints().at(0);
        String url = "http://" + endpoint.ipv4() + ":" + endpoint.port() + "/wot/lookup/" + query;
        StringRequest request = new StringRequest(
                url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        WotLookup lookup = WotLookup.fromJson(response);
                        ((LookupAdapter) mListView.getAdapter()).swapData(lookup);
                        mProgressBar.setVisibility(View.GONE);
                        mListView.setVisibility(View.VISIBLE);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        mProgressBar.setVisibility(View.GONE);
                        mListView.setVisibility(View.VISIBLE);

                        if (error instanceof NoConnectionError) {
                            Toast.makeText(Application.getContext(),
                                    getResources().getString(R.string.no_connection),
                                    Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(Application.getContext(), error.toString(), Toast.LENGTH_LONG).show();
                        }

                    }
                });
        request.setTag(this);
        int socketTimeout = 30000;//30 seconds - change to what you want
        RetryPolicy policy = new DefaultRetryPolicy(socketTimeout, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
        request.setRetryPolicy(policy);
        Application.getRequestQueue().add(request);

        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        return true;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Intent intent = new Intent();
        WotLookup.Result result = (WotLookup.Result) mListView.getItemAtPosition(position);
        intent.putExtra(WotLookup.Result.class.getSimpleName(), result);
        setResult(RESULT_OK, intent);
        finish();
    }
}
