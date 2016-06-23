package com.freelancers.rohan;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;

public class OrderActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    SalesmanRecyclerViewAdapter salesmanRecyclerViewAdapter;
    String Response = "";
    ArrayList<String> product = new ArrayList<>(), number = new ArrayList<>(), company = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order);

        setUpRecyclerView();

        new TallyData().execute();


    }

    private void setUpRecyclerView() {

        recyclerView = (RecyclerView) findViewById(R.id.orderList);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(OrderActivity.this);
        recyclerView.setLayoutManager(layoutManager);
        salesmanRecyclerViewAdapter = new SalesmanRecyclerViewAdapter();
        recyclerView.setAdapter(salesmanRecyclerViewAdapter);
    }
    Dialog dialog;

    class SalesmanRecyclerViewAdapter extends RecyclerView.Adapter<SalesmanRecyclerViewAdapter.MyHolder> {
        @Override
        public MyHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new MyHolder(LayoutInflater.from(OrderActivity.this).inflate(R.layout.single_item, parent, false));
        }

        @Override
        public void onBindViewHolder(MyHolder holder, int position) {
            holder.product.setText(product.get(position) + " - " + company.get(position));
            holder.qty.setText(number.get(position));
            clickCallback(holder, position);
        }

        private void clickCallback(MyHolder holder, final int position) {
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    dialog = new Dialog(OrderActivity.this);
                    dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
//                    dialog.setCancelable(false);
                    dialog.setContentView(R.layout.custom_dialog);

                    TextView productTv = (TextView) dialog.findViewById(R.id.product);
                    final EditText quantity = (EditText) dialog.findViewById(R.id.quantity);
                    final RadioGroup payment = (RadioGroup) dialog.findViewById(R.id.payment);
                    Button cancel = (Button) dialog.findViewById(R.id.cancel);
                    Button accept = (Button) dialog.findViewById(R.id.accept);

                    productTv.setText(product.get(position) + " - " + company.get(position));
                    cancel.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            dialog.dismiss();
                        }
                    });

                    accept.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (quantity.getText().toString().equals("")) {
                                quantity.setError("Cannot be empty");
                                quantity.requestFocus();
                            } else {
                                int selectedId = payment.getCheckedRadioButtonId();

                                RadioButton selected = (RadioButton) payment.findViewById(selectedId);
                                Log.d("testing", quantity.getText().toString() + " " + selectedId + " " + selected.getText().toString());

                                new SendOrder(selected.getText().toString(), quantity.getText().toString(), product.get(position) + " - " + company.get(position)).execute();

                            }
                        }
                    });

                    dialog.show();

                }
            });
        }

        @Override
        public int getItemCount() {
            return product.size();
        }

        class MyHolder extends RecyclerView.ViewHolder {

            TextView product, qty;

            public MyHolder(View itemView) {
                super(itemView);
                product = (TextView) itemView.findViewById(R.id.product);
                qty = (TextView) itemView.findViewById(R.id.qty);
            }
        }
    }


    public class SendOrder extends AsyncTask<Void, Void, Void> {

        String product_name, payment_status, quantity;
        ProgressDialog progressDialog;

        SendOrder(String ps, String qty, String product) {
            product_name = product;
            payment_status = ps;
            quantity = qty;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = ProgressDialog.show(OrderActivity.this, "",
                    "Loading. Please wait...", true);
            progressDialog.setCancelable(true);

        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            progressDialog.dismiss();
            dialog.dismiss();
        }


        @Override
        protected Void doInBackground(Void... params) {
            URL url;
            try {

                url = new URL(Constants.URL+"insert_product.php");
                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();

                httpURLConnection.setDoInput(true);
                httpURLConnection.setDoOutput(true);
                httpURLConnection.setRequestMethod("POST");

                Uri.Builder builder = new Uri.Builder()
                        .appendQueryParameter("admin_id", getSharedPreferences("login", Context.MODE_PRIVATE).getString("admin_id", "0"))
                        .appendQueryParameter("salesman_id", getSharedPreferences("login", Context.MODE_PRIVATE).getString("id", "0"))
                        .appendQueryParameter("product_q", quantity)
                        .appendQueryParameter("payment_status", payment_status)
                        .appendQueryParameter("product_name", product_name);


                String query = builder.build().getEncodedQuery();

                Log.d("test", query);

                OutputStream os = httpURLConnection.getOutputStream();

                BufferedWriter mBufferedWriter = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
                mBufferedWriter.write(query);
                mBufferedWriter.flush();
                mBufferedWriter.close();
                os.close();

                httpURLConnection.connect();
                BufferedReader mBufferedInputStream = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream()));
                String inline;
                while ((inline = mBufferedInputStream.readLine()) != null) {
                    Response += inline;
                }
                mBufferedInputStream.close();
                Log.d("response", Response);

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            } catch (ProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            Response = "";

            return null;
        }
    }

    public class TallyData extends AsyncTask<Void, Void, Void> {

        ProgressDialog progressDialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = ProgressDialog.show(OrderActivity.this, "",
                    "Loading. Please wait...", true);
            progressDialog.setCancelable(true);

        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            salesmanRecyclerViewAdapter.notifyDataSetChanged();
            progressDialog.dismiss();
        }

        @Override
        protected Void doInBackground(Void... params) {

            xmlRequest();

            return null;
        }
    }


    public void xmlRequest() {
        URL url;
        try {
            url = new URL("http://192.168.0.103:9000");
            HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();

            httpURLConnection.setDoInput(true);
            httpURLConnection.setDoOutput(true);
            httpURLConnection.setRequestMethod("POST");


            String query = "<ENVELOPE>\n" +
                    "<HEADER>\n" +
                    "\t<VERSION>1</VERSION>\n" +
                    "\t<TALLYREQUEST>Export</TALLYREQUEST>\n" +
                    "\t<TYPE>Data</TYPE>\n" +
                    "\t<ID>JSON Inventory Details</ID>\n" +
                    "</HEADER>\n" +
                    "<BODY>\n" +
                    "<DESC>\n" +
                    "\t<STATICVARIABLES>\n" +
                    "\t\t<EXPLODEFLAG>Yes</EXPLODEFLAG>\n" +
                    "\t\t<SVEXPORTFORMAT>$$SysName:XML</SVEXPORTFORMAT>\n" +
                    "\t</STATICVARIABLES>\n" +
                    "</DESC>\n" +
                    "</BODY>\n" +
                    "</ENVELOPE>";

            OutputStream os = httpURLConnection.getOutputStream();

            BufferedWriter mBufferedWriter = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
            mBufferedWriter.write(query);
            mBufferedWriter.flush();
            mBufferedWriter.close();
            os.close();

            httpURLConnection.connect();
            BufferedReader mBufferedInputStream = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream()));
            String inline;
            while ((inline = mBufferedInputStream.readLine()) != null) {
                Response += inline;
            }
            mBufferedInputStream.close();
            Log.d("response", Response);

            parseXml();

            Response = "";

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (ProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void parseXml() {
        try {
            XmlPullParserFactory factory = XmlPullParserFactory
                    .newInstance();
            factory.setNamespaceAware(true);
            XmlPullParser parser = factory.newPullParser();
            parser.setInput(new StringReader(Response));

            int eventType = parser.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {

                String name = null;
                switch (eventType) {
                    case XmlPullParser.START_TAG:
                        name = parser.getName();
                        if (name.equals("NAME")) {
                            product.add(parser.nextText());
                        }
                        if (name.equals("STOCKGROUP")) {
                            company.add(parser.nextText());
                        }
                        if (name.equals("QTY")) {
                            number.add(parser.nextText());
                        }
                        break;
                    case XmlPullParser.END_TAG:
                        break;
                }
                eventType = parser.next();
            }


        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }


}
