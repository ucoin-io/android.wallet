package io.ucoin.app.model.http_api;

import com.google.gson.Gson;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Serializable;
import io.ucoin.app.technical.StandardCharsets;
import io.ucoin.app.technical.gson.GsonUtils;

public class Block implements Serializable {

    public String version;
    public Integer nonce;
    public Integer number;
    public Integer powMin;
    public Long time;
    public Long medianTime;
    public Long membersCount;
    public Long monetaryMass;
    public String currency;
    public String issuer;
    public String signature;
    public String hash;
    public String parameters;
    public String previousHash;
    public String previousIssuer;
    public Long dividend;
    public String[] membersChanges;
    public String[] identities;
    public String[] joiners;
    public String[] leavers;
    public String[] excluded;
    public String[] certifications;
    //todo transactions


    public static Block fromJson(InputStream json) {
        Gson gson = GsonUtils.newBuilder().create();
        Reader reader = new InputStreamReader(json, StandardCharsets.UTF_8);
        return gson.fromJson(reader, Block.class);
    }

    public String toString() {
        String s = "version=" + version;
        s += "\nnonce=" + nonce;
        s += "\nnumber=" + number;
        s += "\npowMin" + powMin;
        s += "\ntime=" + time;
        s += "\nmedianTime=" + medianTime;
        s += "\nmembersCount=" + membersCount;
        s += "\nmonetaryMass=" + monetaryMass;
        s += "\ncurrency=" + currency;
        s += "\nissuer=" + issuer;
        s += "\nsignature=" + signature;
        s += "\nhash=" + hash;
        s += "\nparameters=" + parameters;
        s += "\npreviousHash=" + previousHash;
        s += "\npreviousIssuer=" + previousIssuer;
        s += "\ndividend=" + dividend;
        s += "\nmembersChanges:";
        for(String m : membersChanges) {
            s += "\n\t" + m;
        }
        s += "\nidentities:";
        for(String i : identities) {
            s += "\n\t" + i;
        }
        s += "\njoiners:";
        for(String j : joiners) {
            s += "\n\t" + j;
        }
        s += "\nleavers:";
        for(String l : leavers) {
            s += "\n\t" + l;
        }
        s += "\nexcluded:";
        for(String e : excluded) {
            s += "\n\t" + e;
        }
        s += "\ncertifications:";
        for(String c : certifications) {
            s += "\n\t" + c;
        }

        return s;
    }
}