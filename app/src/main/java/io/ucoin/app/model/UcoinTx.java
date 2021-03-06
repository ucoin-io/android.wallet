package io.ucoin.app.model;

import io.ucoin.app.enumeration.DayOfWeek;
import io.ucoin.app.enumeration.Month;
import io.ucoin.app.enumeration.TxDirection;
import io.ucoin.app.enumeration.TxState;

public interface UcoinTx extends SqlRow {
    Long walletId();

    Integer version();

    String comment();

    String hash();

    Long block();

    Long time();

    TxDirection direction();

    TxState state();

    String currencyName();

    Integer year();

    Month month();

    DayOfWeek dayOfWeek();

    Integer day();

    String hour();

    Long quantitativeAmount();

    Double relativeAmountThen();

    Double relativeAmountNow();

    void setComment(String comment);

    void setState(TxState state);

    void setHash(String hash);

    void setTime(Long time);

    void setBlock(Long block);

    UcoinTxIssuers issuers();

    UcoinTxInputs inputs();

    UcoinTxOutputs outputs();

    UcoinTxSignatures signatures();

    UcoinWallet wallet();
}

