package com.tokenbrowser.model.sofa;

import com.tokenbrowser.crypto.util.TypeConverter;
import com.tokenbrowser.util.EthUtil;
import com.tokenbrowser.view.BaseApplication;
import com.squareup.moshi.Json;

import java.math.BigDecimal;
import java.math.BigInteger;

public class Payment {

    private String value;
    private String toAddress;
    private String fromAddress;
    private String txHash;
    private String status;

    @Json(name = SofaType.LOCAL_ONLY_PAYLOAD)
    private ClientSideCustomData androidClientSideCustomData;

    public Payment() {}

    public Payment setValue(final String value) {
        this.value = value;
        generateLocalPrice();
        return this;
    }

    public String getValue() {
        return this.value;
    }

    public String getToAddress() {
        return this.toAddress;
    }

    public Payment setToAddress(final String toAddress) {
        this.toAddress = toAddress;
        return this;
    }

    public String getFromAddress() {
        return fromAddress;
    }

    public Payment setFromAddress(final String ownerAddress) {
        this.fromAddress = ownerAddress;
        return this;
    }

    public String getTxHash() {
        return this.txHash;
    }

    public Payment setTxHash(final String txHash) {
        this.txHash = txHash;
        return this;
    }

    public String getStatus() {
        return this.status;
    }

    public Payment setStatus(final String status) {
        this.status = status;
        return this;
    }


    private Payment setLocalPrice(final String localPrice) {
        if (this.androidClientSideCustomData == null) {
            this.androidClientSideCustomData = new ClientSideCustomData();
        }

        this.androidClientSideCustomData.localPrice = localPrice;
        return this;
    }

    public String getLocalPrice() {
        if (this.androidClientSideCustomData == null) {
            return null;
        }

        return this.androidClientSideCustomData.localPrice;
    }

    public void generateLocalPrice() {
        final BigInteger weiAmount = TypeConverter.StringHexToBigInteger(this.value);
        final BigDecimal ethAmount = EthUtil.weiToEth(weiAmount);
        final String localAmount = BaseApplication.get().getTokenManager().getBalanceManager().convertEthToLocalCurrencyString(ethAmount);
        setLocalPrice(localAmount);
    }

    public String toUserVisibleString() {
        return String.format("%s %s", "Payment", getLocalPrice());
    }

    private static class ClientSideCustomData {
        private String localPrice;
    }
}