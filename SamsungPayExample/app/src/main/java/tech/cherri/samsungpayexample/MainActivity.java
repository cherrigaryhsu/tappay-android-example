package tech.cherri.samsungpayexample;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import tech.cherri.tpdirect.api.TPDCard;
import tech.cherri.tpdirect.api.TPDCardInfo;
import tech.cherri.tpdirect.api.TPDMerchant;
import tech.cherri.tpdirect.api.TPDSamsungPay;
import tech.cherri.tpdirect.api.TPDServerType;
import tech.cherri.tpdirect.api.TPDSetup;
import tech.cherri.tpdirect.callback.TPDSamsungPayStatusListener;
import tech.cherri.tpdirect.callback.TPDTokenFailureCallback;
import tech.cherri.tpdirect.callback.TPDTokenSuccessCallback;


public class MainActivity extends AppCompatActivity implements View.OnClickListener, TPDSamsungPayStatusListener, TPDTokenSuccessCallback, TPDTokenFailureCallback {
    private static final String TAG = "MainActivity";

    private TPDCard.CardType[] allowedNetworks = new TPDCard.CardType[]{TPDCard.CardType.Visa
            , TPDCard.CardType.MasterCard};

    private static final int REQUEST_READ_PHONE_STATE = 101;
    private TPDSamsungPay tpdSamsungPay;
    private ImageView samsungPayBuyBTN;
    private TextView totalAmountTV, samsungPayResultStateTV;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupViews();

        Log.d(TAG, "SDK version is " + TPDSetup.getVersion());

        //Setup environment.
        TPDSetup.initInstance(getApplicationContext(),
                Integer.parseInt(getString(R.string.global_test_app_id)), getString(R.string.global_test_app_key), TPDServerType.Sandbox);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            requestPermissions();
        } else {
            prepareSamsungPay();
        }

    }

    private void setupViews() {
        totalAmountTV = (TextView) findViewById(R.id.totalAmountTV);
        totalAmountTV.setText("Total amount : 1.00 元");

        samsungPayBuyBTN = (ImageView) findViewById(R.id.samsungPayBuyBTN);
        samsungPayBuyBTN.setOnClickListener(this);

        samsungPayResultStateTV = (TextView) findViewById(R.id.samsungPayResultStateTV);
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private void requestPermissions() {
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "PERMISSION IS ALREADY GRANTED");
            prepareSamsungPay();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_PHONE_STATE}, REQUEST_READ_PHONE_STATE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_READ_PHONE_STATE:
                if ((grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    Log.d(TAG, "PERMISSION_GRANTED");
                }
                prepareSamsungPay();
                break;
            default:
                break;
        }
    }

    public void prepareSamsungPay() {
        TPDMerchant tpdMerchant = new TPDMerchant();
        tpdMerchant.setMerchantName("TapPay Samsung Pay Demo");
        tpdMerchant.setSupportedNetworks(allowedNetworks);
        tpdMerchant.setSamsungMerchantId(getString(R.string.global_test_samsung_merchant_id));
        tpdMerchant.setCurrencyCode("TWD");

        tpdSamsungPay = new TPDSamsungPay(this, getString(R.string.global_test_samsung_pay_service_id_sandbox), tpdMerchant);
        tpdSamsungPay.isSamsungPayAvailable(this);
    }


    @Override
    public void onReadyToPayChecked(boolean isReadyToPay, String msg) {
        Log.d(TAG, "Samsung Pay availability : " + isReadyToPay);
        if (isReadyToPay) {
            samsungPayBuyBTN.setVisibility(View.VISIBLE);
        } else {
            showMessage("Cannot use Samsung Pay.");
        }
    }


    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.samsungPayBuyBTN) {
            showProgressDialog();
            tpdSamsungPay.getPrime("1", "0", "0", "1", this, this);
        }
    }

    @Override
    public void onSuccess(String prime, TPDCardInfo cardInfo) {
        hideProgressDialog();

        String resultStr = "Your prime is " + prime
                + "\n\nUse below cURL to proceed the payment : \n"
                + ApiUtil.generatePayByPrimeCURLForSandBox(prime,
                getString(R.string.global_test_partnerKey),
                getString(R.string.global_test_merchant_id));

        showMessage(resultStr);
        Log.d(TAG, resultStr);
    }

    @Override
    public void onFailure(int status, String reportMsg) {
        hideProgressDialog();
        showMessage("TapPay getPrime failed , status = " + status + ", msg : " + reportMsg);
        Log.d("TPDirect createToken", "failure : " + status + ", msg : " + reportMsg);
    }

    private void showMessage(String s) {
        samsungPayResultStateTV.setText(s);
    }


    public ProgressDialog mProgressDialog;

    protected void showProgressDialog() {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(this);
            mProgressDialog.setIndeterminate(true);
            mProgressDialog.setMessage("Loading...");
        }

        mProgressDialog.show();
    }

    protected void hideProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
    }
}
