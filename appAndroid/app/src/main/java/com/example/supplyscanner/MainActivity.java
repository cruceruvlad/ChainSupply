package com.example.supplyscanner;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import com.mongodb.lang.NonNull;
import com.mongodb.stitch.android.core.Stitch;
import com.mongodb.stitch.android.core.StitchAppClient;
import com.mongodb.stitch.android.core.auth.StitchUser;
import com.mongodb.stitch.android.services.mongodb.remote.RemoteMongoClient;
import com.mongodb.stitch.android.services.mongodb.remote.RemoteMongoCollection;
import com.mongodb.stitch.core.auth.providers.anonymous.AnonymousCredential;
import com.mongodb.stitch.core.services.mongodb.remote.RemoteInsertOneResult;
import com.mongodb.stitch.core.services.mongodb.remote.RemoteUpdateOptions;
import com.mongodb.stitch.core.services.mongodb.remote.RemoteUpdateResult;

import org.bson.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    StitchAppClient client;
    RemoteMongoClient mongoClient;
    RemoteMongoCollection<Document> coll;
    EditText numEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final Button buyBtn = (Button) findViewById(R.id.buyBtn);
        final Button sellBtn = (Button) findViewById(R.id.sellBtn);
        numEditText = (EditText)findViewById(R.id.numEditText);
        final Activity activity = this;
        client = Variables.getAppVariable();
        mongoClient = Variables.getServiceVariable();

        numEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if(actionId == EditorInfo.IME_ACTION_DONE){
                    InputMethodManager keyboard = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
                    keyboard.hideSoftInputFromWindow(numEditText.getWindowToken(), 0);
                    if (Integer.parseInt(numEditText.getText().toString()) == 0) {
                        numEditText.setText("1");
                        Toast.makeText(activity, "# of Products cannot be 0", Toast.LENGTH_LONG).show();
                    }
                    return true;
                }
                return false;
            }
        });

        buyBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                IntentIntegrator scanner = new IntentIntegrator(activity);
                scanner.setDesiredBarcodeFormats(IntentIntegrator.ALL_CODE_TYPES);
                scanner.setPrompt("Scan");
                scanner.setCameraId(0);
                scanner.setBeepEnabled(true);
                scanner.setBarcodeImageEnabled(true);
                Variables.setActionVariable(1);
                scanner.initiateScan();
            }
        });

        sellBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                IntentIntegrator scanner = new IntentIntegrator(activity);
                scanner.setDesiredBarcodeFormats(IntentIntegrator.ALL_CODE_TYPES);
                scanner.setPrompt("Scan");
                scanner.setCameraId(0);
                scanner.setBeepEnabled(true);
                scanner.setBarcodeImageEnabled(true);
                Variables.setActionVariable(2);
                scanner.initiateScan();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode,resultCode,data);
        Activity activity = this;
        if(result != null){
            if(result.getContents()==null)
                Toast.makeText(this, "You cancelled the scanning", Toast.LENGTH_LONG).show();
            else {
                coll =
                        mongoClient.getDatabase("supplydb").getCollection("products");
                client.getAuth().loginWithCredential(new AnonymousCredential()).continueWithTask(
                        new Continuation<StitchUser, Task<List<Document>>>() {

                            @Override
                            public Task<List<Document>> then(@NonNull Task<StitchUser> task) throws Exception {
                                List<Document> docs = new ArrayList<>();
                                return coll
                                        .find(new Document("_id", result.getContents().toString()))
                                        .limit(1)
                                        .into(docs);
                            }
                        }
                ).continueWithTask(new Continuation<List<Document>, Task<RemoteUpdateResult>>() {
                    @Override
                    public Task<RemoteUpdateResult> then(@NonNull Task<List<Document>> task) throws Exception {
                        if (!task.isSuccessful()) {
                            Toast.makeText(activity,"Unexpected Error", Toast.LENGTH_LONG).show();
                            throw task.getException();
                        }
                        if (task.getResult().isEmpty()) {
                            Toast.makeText(activity, "There is no such product", Toast.LENGTH_LONG).show();
                            return null;
                        }
                        final Document toBeUpdated = task.getResult().get(0);
                        int stock = toBeUpdated.getInteger("stock");
                        final String id = toBeUpdated.getString("_id");
                        final int num = Integer.parseInt(numEditText.getText().toString());
                        if(Variables.getActionVariable() == 1)
                            stock += num;
                        else{
                            if(num > stock) {
                                Toast.makeText(activity, "Insufficient stock", Toast.LENGTH_LONG).show();
                                return null;
                            }
                            stock -= num;
                        }
                        toBeUpdated.put("stock", stock);
                        return coll.updateOne(new Document("_id",id.toString()), toBeUpdated, new RemoteUpdateOptions().upsert(true));
                    }
                }).addOnCompleteListener(new OnCompleteListener<RemoteUpdateResult>() {
                    @Override
                    public void onComplete(@NonNull Task<RemoteUpdateResult> task) {
                        if (!task.isSuccessful()) {
                            Toast.makeText(activity, "Fail", Toast.LENGTH_LONG).show();
                            return;
                        }
                        Toast.makeText(activity,"Succes", Toast.LENGTH_LONG).show();
                    }
                });
            }
        }
        else
            super.onActivityResult(requestCode, resultCode, data);
    }
}

class Variables extends Application{
    private static StitchAppClient client = Stitch.initializeDefaultAppClient("supplyapp-cvejh");
    private static RemoteMongoClient mongoClient = client.getServiceClient(RemoteMongoClient.factory, "supplyapp");
    private static int action = 0;

    public static StitchAppClient getAppVariable() {
        return client;
    }

    public static RemoteMongoClient getServiceVariable(){
        return mongoClient;
    }

    public static void setActionVariable(int a){
        action = a;
    }

    public static int getActionVariable(){
        return action;
    }
}