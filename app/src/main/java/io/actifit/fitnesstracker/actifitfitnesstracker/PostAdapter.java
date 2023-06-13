package io.actifit.fitnesstracker.actifitfitnesstracker;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.Image;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.mittsu.markedview.MarkedView;
import com.scottyab.rootbeer.Const;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public class PostAdapter extends ArrayAdapter<SingleHivePostModel> {

    String username, pkey;
    //JSONArray consumedProducts;
    ProgressDialog progress;
    Context ctx;

    public PostAdapter(Context context, ArrayList<SingleHivePostModel> post,
                       String username, String pkey){
        super(context, 0, post);


    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        try {

            // Get the data item for this position
            final SingleHivePostModel postEntry = getItem(position);
            // Check if an existing view is being reused, otherwise inflate the view
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.post_entry, parent, false);
            }

            // Lookup view for data population

            CardView entryContainer = convertView.findViewById(R.id.entryContainer);
            TextView title = convertView.findViewById(R.id.title);
            //TextView body = convertView.findViewById(R.id.body);
            //TextView permlink = convertView.findViewById(R.id.permlink);
            TextView author = convertView.findViewById(R.id.author);
            final ImageView userProfilePic = convertView.findViewById(R.id.author_pic);
            TextView date = convertView.findViewById(R.id.date);
            ImageView mainImage = convertView.findViewById(R.id.post_image);
            MarkedView mdView = convertView.findViewById(R.id.md_view);
            TextView upvoteCount = convertView.findViewById(R.id.upvote_count);
            TextView commentCount = convertView.findViewById(R.id.comment_count);
            Button expandButton = convertView.findViewById(R.id.expand_button);
            Button retractButton = convertView.findViewById(R.id.retract_button);

            // Populate the data into the template view using the data object

            //checkmark
            String checkMark = "&#10003;";//"&#9989;";//"✅";
            String xMark = "&#10006;";//"&#10060;";//"❌";
            int colorSuccess = getContext().getResources().getColor(R.color.actifitDarkGreen);
            int colorFail = getContext().getResources().getColor(R.color.actifitRed);


            //map post content
            title.setText(postEntry.title);
            //permlink.setText(postEntry.permlink);

            //body.setText(postEntry.body);
            //convert markdown
            String shortenedContent = Utils.parseMarkdown(postEntry.body);
            //removed extra tags
            shortenedContent = Utils.sanitizeContent(shortenedContent, false);

            shortenedContent = Utils.trimText(shortenedContent, Constants.trimmedTextSize);

            //to be used when setting value upon content retract
            final String finalShortenedContent = shortenedContent;

            mdView.setMDText(finalShortenedContent);




            author.setText('@'+postEntry.author);
            date.setText(Utils.getTimeDifference(postEntry.created));

            upvoteCount.setText(postEntry.active_votes.length()+"");
            commentCount.setText(postEntry.children+"");

            //profile pic
            final String userImgUrl = getContext().getString(R.string.hive_image_host_url).replace("USERNAME", postEntry.author);


            //fetch main post image
            String fetchedImageUrl = "";
            try {
                JSONObject jsonMetadata = postEntry.json_metadata;
                if (jsonMetadata.has("image")) {
                    JSONArray imageArray = jsonMetadata.getJSONArray("image");
                    if (imageArray.length() > 0) {
                        fetchedImageUrl = imageArray.getString(0);
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

            final String mainImageUrl = fetchedImageUrl;

            Handler uiHandler = new Handler(Looper.getMainLooper());
            uiHandler.post(new Runnable(){
                @Override
                public void run() {
                    //Picasso.with(ctx)
                    //load user image
                    Picasso.get()
                            .load(userImgUrl)
                            .into(userProfilePic);

                    Picasso.get()
                            .load(mainImageUrl)
                            .into(mainImage);
                }
            });



            ScaleAnimation scaler;
            scaler = new ScaleAnimation(1f, 0.8f, 1f,0.8f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
            scaler.setDuration(500);
            scaler.setRepeatMode(Animation.REVERSE);
            scaler.setRepeatCount(Animation.INFINITE);


            expandButton.setOnClickListener(view -> {
                //expand text visibility
                expandButton.setVisibility(View.INVISIBLE);
                retractButton.setVisibility(View.VISIBLE);
                mdView.setMDText(Utils.sanitizeContent(postEntry.body, true));

                ViewGroup.LayoutParams layoutParams = mdView.getLayoutParams();
                layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;

                mdView.setLayoutParams(layoutParams);

                mainImage.setVisibility(View.GONE);
            });

            retractButton.setOnClickListener(view -> {
                //retract text visibility
                expandButton.setVisibility(View.VISIBLE);
                retractButton.setVisibility(View.INVISIBLE);

                mdView.setMDText(finalShortenedContent);
                mainImage.setVisibility(View.VISIBLE);
            });

            //activate gadget functionality
            /*
            activateGadget.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {


                    if (username == null || username.length() <1){
                        Toast.makeText(getContext(), getContext().getString(R.string.username_missing), Toast.LENGTH_LONG).show();
                        return;
                    }

                    activateGadget.startAnimation(scaler);
                    //buyAFIT.animate().scaleX(0.5f).scaleY(0.5f).setDuration(3000).;
                    //buyAFIT.animate().scaleXBy(1).setDuration(3000); //.startAnimation();

                    //progress.setMessage(getContext().getString(R.string.processingBuyGadget));
                    //progress.show();

                    RequestQueue queue = Volley.newRequestQueue(getContext());

                    //first make sure if user is properly logged in as we need to connect to server
                    if (LoginActivity.accessToken.equals("")){

                        //authorize user login based on credentials if user is already verified
                        if (!pkey.equals("")) {
                            String loginAuthUrl = getContext().getString(R.string.live_server)
                                    + getContext().getString(R.string.login_auth);


                            JSONObject loginSettings = new JSONObject();
                            try {
                                loginSettings.put(getContext().getString(R.string.username_param), MainActivity.username);
                                loginSettings.put(getContext().getString(R.string.pkey_param), pkey);
                                loginSettings.put(getContext().getString(R.string.bchain_param), "HIVE");//default always HIVE
                                loginSettings.put(getContext().getString(R.string.keeploggedin_param), false);//TODO make dynamic
                                loginSettings.put(getContext().getString(R.string.login_source), getContext().getString(R.string.android) + BuildConfig.VERSION_NAME);
                            } catch (JSONException e) {
                                //Log.e(MainActivity.TAG, e.getMessage());
                            }

                            //grab auth token for logged in user
                            JsonObjectRequest loginRequest = new JsonObjectRequest(Request.Method.POST,
                                    loginAuthUrl, loginSettings,
                                    new Response.Listener<JSONObject>() {

                                        @Override
                                        public void onResponse(JSONObject response) {
                                            //store token for reuse when saving settings
                                            try {
                                                if (response.has("success")) {
                                                    Log.d(MainActivity.TAG, response.toString());
                                                    LoginActivity.accessToken = response.getString(getContext().getString(R.string.login_token));
                                                }
                                            } catch (JSONException e) {
                                                e.printStackTrace();
                                            }
                                        }

                                    },
                                    new Response.ErrorListener() {
                                        @Override
                                        public void onErrorResponse(VolleyError error) {
                                            // error
                                            Log.e(MainActivity.TAG, "Login error");
                                        }
                                    });

                            queue.add(loginRequest);

                        }


                    }

                    //prepare query and broadcast to bchain

                    //param 1
                    String op_name = "custom_json";

                    //param 2
                    JSONObject cstm_params = new JSONObject();
                    try {

                        JSONArray required_auths= new JSONArray();

                        JSONArray required_posting_auths = new JSONArray();
                        required_posting_auths.put(MainActivity.username);

                        //cstm_params.put("required_auths", "[]");
                        cstm_params.put("required_auths", required_auths);
                        cstm_params.put("required_posting_auths", required_posting_auths);
                        cstm_params.put("id", "actifit");
                        //cstm_params.put("json", json_op_details);
                        if (!postEntry.isFriendRewarding){
                            cstm_params.put("json", "{\"transaction\": \"activate-gadget\" , \"gadget\": \"" + postEntry.id + "\"}");
                        }else{
                            String friendBenefic = friendBeneficiary.getText().toString();
                            if (friendBenefic.equals("")){
                                //send out error
                                Toast.makeText(getContext(), getContext().getString(R.string.error_activate_product_benefic),  Toast.LENGTH_LONG).show();
                                activateGadget.clearAnimation();
                                return;
                            }
                            cstm_params.put("json", "{\"transaction\": \"activate-gadget\" , \"gadget\": \"" + postEntry.id + "\" , \"benefic\": \"" + friendBenefic + "\"}");
                        }

                        JSONArray operation = new JSONArray();
                        operation.put(0, op_name);
                        operation.put(1, cstm_params);

                        String bcastUrl = getContext().getString(R.string.perform_trx_link) +
                                MainActivity.username +
                                "&operation=[" + operation + "]" +
                                "&bchain=HIVE";//hardcoded for now
                        ;


                        //send out transaction
                        JsonObjectRequest transRequest = new JsonObjectRequest(Request.Method.GET,
                                bcastUrl, null,
                                new Response.Listener<JSONObject>() {

                                    @Override
                                    public void onResponse(JSONObject response) {

                                        Log.d(MainActivity.TAG, response.toString());
                                        //
                                        if (response.has("success")){
                                            //successfully wrote to chain gadget purchase
                                            try {
                                                JSONObject bcastRes = response.getJSONObject("trx").getJSONObject("tx");



                                                String buyUrl = getContext().getString(R.string.activate_gadget_link)+
                                                        MainActivity.username+"/"+
                                                        postEntry.id+"/"+
                                                        bcastRes.get("ref_block_num")+"/"+
                                                        bcastRes.get("id")+"/"+
                                                        "HIVE";

                                                if (postEntry.isFriendRewarding){
                                                    //append friend beneficiary
                                                    buyUrl += "/"+ friendBeneficiary.getText().toString();
                                                }


                                                //send out transaction
                                                JsonObjectRequest buyRequest = new JsonObjectRequest(Request.Method.GET,
                                                        buyUrl, null,
                                                        new Response.Listener<JSONObject>() {

                                                            @Override
                                                            public void onResponse(JSONObject response) {
                                                                //progress.dismiss();
                                                                activateGadget.clearAnimation();
                                                                Log.d(MainActivity.TAG, response.toString());
                                                                //
                                                                if (!response.has("error")) {
                                                                    //showActivateButton(postEntry, finalConvertView);
                                                                    postEntry.nonConsumedCopy = SingleProductModel.ACTIVECOPY;
                                                                    friendBeneficiary.setVisibility(View.GONE);
                                                                    activateGadget.setVisibility(View.GONE);
                                                                    deactivateGadget.setVisibility(View.VISIBLE);
                                                                    //successfully bought product
                                                                    Toast.makeText(getContext(), getContext().getString(R.string.success_activate_product)+ " " +postEntry.name, Toast.LENGTH_LONG).show();
                                                                } else {
                                                                    Toast.makeText(getContext(), getContext().getString(R.string.error_activate_product), Toast.LENGTH_LONG).show();
                                                                }
                                                            }
                                                        },
                                                        new Response.ErrorListener() {
                                                            @Override
                                                            public void onErrorResponse(VolleyError error) {
                                                                // error
                                                                Log.d(MainActivity.TAG, "Error querying blockchain");
                                                                //progress.dismiss();
                                                                activateGadget.clearAnimation();
                                                                Toast.makeText(getContext(), getContext().getString(R.string.error_activate_product), Toast.LENGTH_LONG).show();
                                                            }
                                                        });

                                                queue.add(buyRequest);


                                            } catch (JSONException e) {
                                                e.printStackTrace();
                                            }
                                        }else{
                                            //progress.dismiss();
                                            activateGadget.clearAnimation();
                                            Toast.makeText(getContext(), getContext().getString(R.string.error_activate_product), Toast.LENGTH_LONG).show();
                                        }

                                    }

                                },
                                new Response.ErrorListener() {
                                    @Override
                                    public void onErrorResponse(VolleyError error) {
                                        // error
                                        Log.d(MainActivity.TAG, "Error querying blockchain");
                                        //progress.dismiss();
                                        activateGadget.clearAnimation();
                                        Toast.makeText(getContext(), getContext().getString(R.string.error_activate_product), Toast.LENGTH_LONG).show();
                                    }
                                }) {
                            @NonNull
                            @Override
                            public Map<String, String> getHeaders() throws AuthFailureError {
                                final Map<String, String> params = new HashMap<>();
                                params.put("Content-Type", "application/json");
                                params.put(getContext().getString(R.string.validation_header), getContext().getString(R.string.validation_pre_data) + " " + LoginActivity.accessToken);
                                return params;
                            }
                        };

                        queue.add(transRequest);
                    }  catch (Exception excep) {
                        excep.printStackTrace();
                    }

                }
            });

            */



        }catch(Exception exp){
            exp.printStackTrace();
        }

        // Return the completed view to render on screen
        return convertView;
    }

}