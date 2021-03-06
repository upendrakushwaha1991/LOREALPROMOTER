package com.cpm.upload;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.cpm.Constants.CommonString;
import com.cpm.database.GSKDatabase;
import com.cpm.delegates.CoverageBean;
import com.cpm.lorealpromoter.MainMenuActivity;
import com.cpm.lorealpromoter.R;
import com.cpm.message.AlertMessage;
import com.cpm.xmlGetterSetter.AssetInsertdataGetterSetter;
import com.cpm.xmlGetterSetter.FailureGetterSetter;
import com.cpm.xmlGetterSetter.MarketIntelligenceGetterSetter;
import com.cpm.xmlGetterSetter.PromotionInsertDataGetterSetter;
import com.cpm.xmlGetterSetter.SampledGetterSetter;
import com.cpm.xmlGetterSetter.StockNewGetterSetter;
import com.cpm.xmlHandler.FailureXMLHandler;

import org.ksoap2.SoapEnvelope;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.util.ArrayList;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

@SuppressWarnings("deprecation")
public class UploadDataActivity extends Activity {
    private Dialog dialog;
    private ProgressBar pb;
    private TextView percentage, message;
    String app_ver;
    private String visit_date, username;
    private SharedPreferences preferences;
    private GSKDatabase database;
    String datacheck = "";
    String[] words;
    String validity;
    int mid;
    Data data;
    String Path;
    String errormsg = "";
    boolean isError = false;
    boolean up_success_flag = true;
    String exceptionMessage = "";
    String resultFinal;
    private ArrayList<CoverageBean> coverageBeanlist = new ArrayList<CoverageBean>();
    private FailureGetterSetter failureGetterSetter = null;
    private ArrayList<StockNewGetterSetter> stockData = new ArrayList<>();
    private ArrayList<StockNewGetterSetter> stockbackroomData = new ArrayList<>();
    private ArrayList<StockNewGetterSetter> stockImages = new ArrayList<>();
    private ArrayList<StockNewGetterSetter> stockbackroomImages = new ArrayList<>();
    private ArrayList<PromotionInsertDataGetterSetter> promotionData = new ArrayList<>();
    private ArrayList<AssetInsertdataGetterSetter> paidVisibility = new ArrayList<>();
    ArrayList<MarketIntelligenceGetterSetter> additionalVisibilityData = new ArrayList<>();
    ArrayList<SampledGetterSetter> sampledData = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        visit_date = preferences.getString(CommonString.KEY_DATE, null);
        username = preferences.getString(CommonString.KEY_USERNAME, null);
        app_ver = preferences.getString(CommonString.KEY_VERSION, "");
        database = new GSKDatabase(this);
        database.open();
        Path = CommonString.FILE_PATH;
        new UploadTask(this).execute();
    }

    private class UploadTask extends AsyncTask<Void, Data, String> {
        private Context context;

        UploadTask(Context context) {
            this.context = context;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            dialog = new Dialog(context);
            dialog.setContentView(R.layout.custom_upload);
            dialog.setTitle("Uploading Data");
            dialog.setCancelable(false);
            dialog.show();
            pb = (ProgressBar) dialog.findViewById(R.id.progressBar1);
            percentage = (TextView) dialog.findViewById(R.id.percentage);
            message = (TextView) dialog.findViewById(R.id.message);
        }

        @Override
        protected String doInBackground(Void... params) {

            try {
                data = new Data();
                data.value = 10;
                data.name = "Uploading....";
                publishProgress(data);
                coverageBeanlist = database.getCoverageData(visit_date);
                String final_xml = "";
                String onXML = "";
                if (coverageBeanlist.size() > 0) {
                    for (int i = 0; i < coverageBeanlist.size(); i++) {
                        if (!coverageBeanlist.get(i).getStatus().equalsIgnoreCase(CommonString.KEY_U)) {
                            onXML = "[DATA]" + "[USER_DATA]"
                                    + "[STORE_CD]" + coverageBeanlist.get(i).getStoreId() + "[/STORE_CD]"
                                    + "[VISIT_DATE]" + coverageBeanlist.get(i).getVisitDate() + "[/VISIT_DATE]"
                                    + "[LATITUDE]" + coverageBeanlist.get(i).getLatitude() + "[/LATITUDE]"
                                    + "[APP_VERSION]" + app_ver + "[/APP_VERSION]"
                                    + "[LONGITUDE]" + coverageBeanlist.get(i).getLongitude() + "[/LONGITUDE]"
                                    + "[IN_TIME]" + coverageBeanlist.get(i).getInTime() + "[/IN_TIME]"
                                    + "[OUT_TIME]" + coverageBeanlist.get(i).getOutTime() + "[/OUT_TIME]"
                                    + "[UPLOAD_STATUS]" + "N" + "[/UPLOAD_STATUS]"
                                    + "[USER_ID]" + username + "[/USER_ID]"
                                    + "[IMAGE_URL]" + coverageBeanlist.get(i).getImage() + "[/IMAGE_URL]"
                                    + "[REASON_ID]" + coverageBeanlist.get(i).getReasonid() + "[/REASON_ID]"
                                    + "[REASON_REMARK]" + coverageBeanlist.get(i).getRemark() + "[/REASON_REMARK]"
                                    + "[/USER_DATA]"
                                    + "[/DATA]";

                            SoapObject request = new SoapObject(CommonString.NAMESPACE, CommonString.METHOD_UPLOAD_DR_STORE_COVERAGE);
                            request.addProperty("onXML", onXML);
                            SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
                            envelope.dotNet = true;
                            envelope.setOutputSoapObject(request);
                            HttpTransportSE androidHttpTransport = new HttpTransportSE(CommonString.URL);
                            androidHttpTransport.call(CommonString.SOAP_ACTION + CommonString.METHOD_UPLOAD_DR_STORE_COVERAGE, envelope);
                            Object result = (Object) envelope.getResponse();
                            datacheck = result.toString();
                            datacheck = datacheck.replace("\"", "");
                            words = datacheck.split("\\;");
                            validity = (words[0]);
                            if (validity.equalsIgnoreCase(CommonString.KEY_SUCCESS)) {
                                database.updateCoverageStatus(coverageBeanlist.get(i).getMID(), CommonString.KEY_P);
                                database.updateStoreStatusOnLeave(coverageBeanlist.get(i).getStoreId(), coverageBeanlist.get(i).getVisitDate(), CommonString.KEY_P);
                            } else {
                                isError = true;
                                continue;
                            }
                            mid = Integer.parseInt((words[1]));
                            data.value = 20;
                            data.name = "Uploading";
                            publishProgress(data);

                            //LOREALPRO_STOCK_DATA
                            final_xml = "";
                            onXML = "";
                            stockData = database.getOpeningStockUpload(coverageBeanlist.get(i).getStoreId());
                            if (stockData.size() > 0) {
                                for (int j = 0; j < stockData.size(); j++) {
                                    onXML = "[LOREAL_STOCK_DATA]"
                                            + "[MID]" + mid + "[/MID]"
                                            + "[CREATED_BY]" + username + "[/CREATED_BY]"
                                            + "[SKU_CD]" + stockData.get(j).getSku_cd() + "[/SKU_CD]"
                                            + "[STOCK]" + stockData.get(j).getStock1() + "[/STOCK]"
                                            + "[FACING]" + stockData.get(j).getStock2() + "[/FACING]"
                                            + "[MIDDAY_STOCK]" + stockData.get(j).getEd_midFacing() + "[/MIDDAY_STOCK]"
                                            + "[CLOSING_STOCK]" + stockData.get(j).getEd_closingFacing() + "[/CLOSING_STOCK]"
                                            + "[/LOREAL_STOCK_DATA]";

                                    final_xml = final_xml + onXML;
                                }

                                final String sos_xml = "[DATA]" + final_xml + "[/DATA]";
                                request = new SoapObject(CommonString.NAMESPACE, CommonString.METHOD_UPLOAD_XML);
                                request.addProperty("XMLDATA", sos_xml);
                                request.addProperty("KEYS", "LOREAL_STOCK_DATA");
                                request.addProperty("USERNAME", username);
                                request.addProperty("MID", mid);
                                envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
                                envelope.dotNet = true;
                                envelope.setOutputSoapObject(request);
                                androidHttpTransport = new HttpTransportSE(CommonString.URL);
                                androidHttpTransport.call(CommonString.SOAP_ACTION + CommonString.METHOD_UPLOAD_XML, envelope);
                                result = (Object) envelope.getResponse();
                                if (!result.toString().equalsIgnoreCase(CommonString.KEY_SUCCESS)) {
                                    isError = true;
                                }
                                data.value = 20;
                                data.name = "LOREAL_STOCK_DATA Data";
                                publishProgress(data);
                            }


                            //LOREALPRO_STOCK_IMAGE_DATA
                            final_xml = "";
                            onXML = "";
                            stockImages = database.getStockImageUploadData(coverageBeanlist.get(i).getStoreId());
                            if (stockImages.size() > 0 && !stockImages.get(0).getImg_cam().equals("") ||  (stockImages.size() > 0 && !stockImages.get(0).getCatstock().equals("") )) {
                                for (int j = 0; j < stockImages.size(); j++) {
                                   /* if (!stockImages.get(j).getImg_cam().equals("") || !stockImages.get(j).getImg_cat_one().equals("")
                                            || !stockImages.get(j).getImg_cat_two().equals("")) {
*/
                                        onXML = "[LOREAL_STOCK_CATEGORY_IMAGE_DATA]"
                                                + "[MID]" + mid + "[/MID]"
                                                + "[CREATED_BY]" + username + "[/CREATED_BY]"
                                                + "[CATEGORY_CD]" + stockImages.get(j).getCategory_cd() + "[/CATEGORY_CD]"
                                                + "[CATEGORY_FACING]" + stockImages.get(j).getCatstock() + "[/CATEGORY_FACING]"
                                                + "[CATEGORY_IMAGE]" + stockImages.get(j).getImg_cat_one() + "[/CATEGORY_IMAGE]"
                                                + "[/LOREAL_STOCK_CATEGORY_IMAGE_DATA]";

                                        final_xml = final_xml + onXML;
                                   // }

                                }

                                final String sos_xml = "[DATA]" + final_xml + "[/DATA]";
                                request = new SoapObject(CommonString.NAMESPACE, CommonString.METHOD_UPLOAD_XML);
                                request.addProperty("XMLDATA", sos_xml);
                                request.addProperty("KEYS", "LOREAL_STOCK_CATEGORY_IMAGE_DATA");
                                request.addProperty("USERNAME", username);
                                request.addProperty("MID", mid);
                                envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
                                envelope.dotNet = true;
                                envelope.setOutputSoapObject(request);
                                androidHttpTransport = new HttpTransportSE(CommonString.URL);
                                androidHttpTransport.call(CommonString.SOAP_ACTION + CommonString.METHOD_UPLOAD_XML, envelope);
                                result = (Object) envelope.getResponse();
                                if (!result.toString().equalsIgnoreCase(CommonString.KEY_SUCCESS)) {
                                    isError = true;
                                }
                                data.value = 25;
                                data.name = "Stock Image Data";
                                publishProgress(data);
                            }


                            //LOREAL_STOCK_BACKROOM_DATA
                            final_xml = "";
                            onXML = "";
                            stockbackroomData = database.getOpeningBackRoomStockUpload(coverageBeanlist.get(i).getStoreId());
                            if (stockbackroomData.size() > 0) {
                                for (int j = 0; j < stockbackroomData.size(); j++) {
                                    onXML = "[LOREAL_STOCK_BACKROOM_DATA]"
                                            + "[MID]" + mid + "[/MID]"
                                            + "[CREATED_BY]" + username + "[/CREATED_BY]"
                                            + "[SKU_CD]" + stockbackroomData.get(j).getSku_cd() + "[/SKU_CD]"
                                            + "[STOCK_1]" + stockbackroomData.get(j).getStock1() + "[/STOCK_1]"
                                            + "[STOCK_2]" + stockbackroomData.get(j).getStock2() + "[/STOCK_2]"
                                            + "[CLOSING_STOCK]" + stockbackroomData.get(j).getEd_closingFacing() + "[/CLOSING_STOCK]"
                                            + "[/LOREAL_STOCK_BACKROOM_DATA]";

                                    final_xml = final_xml + onXML;
                                }

                                final String sos_xml = "[DATA]" + final_xml + "[/DATA]";
                                request = new SoapObject(CommonString.NAMESPACE, CommonString.METHOD_UPLOAD_XML);
                                request.addProperty("XMLDATA", sos_xml);
                                request.addProperty("KEYS", "LOREAL_STOCK_BACKROOM_DATA");
                                request.addProperty("USERNAME", username);
                                request.addProperty("MID", mid);
                                envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
                                envelope.dotNet = true;
                                envelope.setOutputSoapObject(request);
                                androidHttpTransport = new HttpTransportSE(CommonString.URL);
                                androidHttpTransport.call(CommonString.SOAP_ACTION + CommonString.METHOD_UPLOAD_XML, envelope);
                                result = (Object) envelope.getResponse();
                                if (!result.toString().equalsIgnoreCase(CommonString.KEY_SUCCESS)) {
                                    isError = true;
                                }
                                data.value = 20;
                                data.name = "LOREAL_STOCK_BACKROOM_DATA Data";
                                publishProgress(data);
                            }


                            //LOREAL_STOCK_BACKROOM_IMAGE_DATA
                            final_xml = "";
                            onXML = "";
                            stockbackroomImages = database.getStockBackRoomImageUploadData(coverageBeanlist.get(i).getStoreId());
                            if (stockbackroomImages.size() > 0 && !stockbackroomImages.get(0).getImg_cam().equals("")) {
                                for (int j = 0; j < stockbackroomImages.size(); j++) {
                                    if (!stockbackroomImages.get(j).getImg_cam().equals("") || !stockbackroomImages.get(j).getImg_cat_one().equals("")
                                            || !stockbackroomImages.get(j).getImg_cat_two().equals("")) {

                                        onXML = "[LOREAL_STOCK_BACKROOM_IMAGE_DATA]"
                                                + "[MID]" + mid + "[/MID]"
                                                + "[CREATED_BY]" + username + "[/CREATED_BY]"
                                                + "[CATEGORY_CD]" + stockbackroomImages.get(j).getCategory_cd() + "[/CATEGORY_CD]"
                                                + "[LOREAL_BACKROOM_IMAGE]" + stockbackroomImages.get(j).getImg_cam() + "[/LOREAL_BACKROOM_IMAGE]"
                                                + "[CATEGORY__ONE_BACKROOM_IMAGE]" + stockbackroomImages.get(j).getImg_cat_one() + "[/CATEGORY__ONE_BACKROOM_IMAGE]"
                                                + "[CATEGORY__TWO_BACKROOM_IMAGE]" + stockbackroomImages.get(j).getImg_cat_two() + "[/CATEGORY__TWO_BACKROOM_IMAGE]"
                                                + "[/LOREAL_STOCK_BACKROOM_IMAGE_DATA]";

                                        final_xml = final_xml + onXML;
                                    }

                                }

                                final String sos_xml = "[DATA]" + final_xml + "[/DATA]";
                                request = new SoapObject(CommonString.NAMESPACE, CommonString.METHOD_UPLOAD_XML);
                                request.addProperty("XMLDATA", sos_xml);
                                request.addProperty("KEYS", "LOREAL_STOCK_BACKROOM_IMAGE_DATA");
                                request.addProperty("USERNAME", username);
                                request.addProperty("MID", mid);
                                envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
                                envelope.dotNet = true;
                                envelope.setOutputSoapObject(request);
                                androidHttpTransport = new HttpTransportSE(CommonString.URL);
                                androidHttpTransport.call(CommonString.SOAP_ACTION + CommonString.METHOD_UPLOAD_XML, envelope);
                                result = (Object) envelope.getResponse();
                                if (!result.toString().equalsIgnoreCase(CommonString.KEY_SUCCESS)) {
                                    isError = true;
                                }
                                data.value = 25;
                                data.name = "LOREAL_STOCK_BACKROOM Image Data";
                                publishProgress(data);
                            }


                            //Promotion Data
                            final_xml = "";
                            onXML = "";
                            promotionData = database.getPromotionUploadData(coverageBeanlist.get(i).getStoreId());
                            if (promotionData.size() > 0) {
                                for (int j = 0; j < promotionData.size(); j++) {
                                    onXML = "[LOREAL_PROMOTION_DATA]"
                                            + "[MID]" + mid + "[/MID]"
                                            + "[CREATED_BY]" + username + "[/CREATED_BY]"
                                            + "[PID]" + promotionData.get(j).getPromotion_id() + "[/PID]"
                                            + "[BRAND_CD]" + promotionData.get(j).getBrand_cd() + "[/BRAND_CD]"
                                            + "[PRESENT_SPIN]" + promotionData.get(j).getPresentSpi() + "[/PRESENT_SPIN]"
                                            + "[CAMERA]" + promotionData.get(j).getCamera() + "[/CAMERA]"
                                            + "[PROMOTION_REMARK_CD]" + promotionData.get(j).getReason_cd() + "[/PROMOTION_REMARK_CD]"
                                            + "[/LOREAL_PROMOTION_DATA]";

                                    final_xml = final_xml + onXML;
                                }

                                final String sos_xml = "[DATA]" + final_xml + "[/DATA]";

                                request = new SoapObject(CommonString.NAMESPACE, CommonString.METHOD_UPLOAD_XML);
                                request.addProperty("XMLDATA", sos_xml);
                                request.addProperty("KEYS", "LOREAL_PROMOTION_DATA");
                                request.addProperty("USERNAME", username);
                                request.addProperty("MID", mid);

                                envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
                                envelope.dotNet = true;
                                envelope.setOutputSoapObject(request);
                                androidHttpTransport = new HttpTransportSE(CommonString.URL);
                                androidHttpTransport.call(CommonString.SOAP_ACTION + CommonString.METHOD_UPLOAD_XML, envelope);
                                result = (Object) envelope.getResponse();
                                if (!result.toString().equalsIgnoreCase(CommonString.KEY_SUCCESS)) {
                                    isError = true;
                                }
                                data.value = 30;
                                data.name = "Promotion Data";
                                publishProgress(data);
                            }


                            //Paid Visibility Data
                            final_xml = "";
                            String paid_visibility = "";
                            onXML = "";
                            paidVisibility = database.getAssetUploadData(coverageBeanlist.get(i).getStoreId());
                            if (paidVisibility.size() > 0) {
                                for (int j = 0; j < paidVisibility.size(); j++) {

                                    onXML = "[LOREAL_PAID_VISIBILITY]"
                                            + "[MID]" + mid + "[/MID]"
                                            + "[CREATED_BY]" + username + "[/CREATED_BY]"
                                            + "[CATEGORY_CD]" + paidVisibility.get(j).getCategory_cd() + "[/CATEGORY_CD]"
                                            + "[ASSET_CD]" + paidVisibility.get(j).getAsset_cd() + "[/ASSET_CD]"
                                            + "[PRESENT]" + paidVisibility.get(j).getPresent() + "[/PRESENT]"
                                            + "[ASSET_REASON_CD]" + paidVisibility.get(j).getReason_cd() + "[/ASSET_REASON_CD]"
                                            + "[IMAGE]" + paidVisibility.get(j).getImg() + "[/IMAGE]"
                                            + "[/LOREAL_PAID_VISIBILITY]";

                                    paid_visibility = paid_visibility + onXML;
                                }
                                final String sos_xml = "[DATA]" + paid_visibility + "[/DATA]";
                                request = new SoapObject(CommonString.NAMESPACE, CommonString.METHOD_UPLOAD_XML);
                                request.addProperty("XMLDATA", sos_xml);
                                request.addProperty("KEYS", "LOREAL_PAID_VISIBILITY");
                                request.addProperty("USERNAME", username);
                                request.addProperty("MID", mid);
                                envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
                                envelope.dotNet = true;
                                envelope.setOutputSoapObject(request);
                                androidHttpTransport = new HttpTransportSE(CommonString.URL);
                                androidHttpTransport.call(CommonString.SOAP_ACTION + CommonString.METHOD_UPLOAD_XML, envelope);
                                result = (Object) envelope.getResponse();

                                if (!result.toString().equalsIgnoreCase(CommonString.KEY_SUCCESS)) {
                                    isError = true;
                                }
                                data.value = 40;
                                data.name = "Paid Visibility Data";
                                publishProgress(data);

                            }


                            final_xml = "";
                            onXML = "";
                            paid_visibility = "";
                            additionalVisibilityData = database.getinsertedMarketIntelligenceData(coverageBeanlist.get(i).getStoreId(), coverageBeanlist.get(i).getVisitDate());
                            if (additionalVisibilityData.size() > 0) {
                                for (int j = 0; j < additionalVisibilityData.size(); j++) {
                                    onXML = "[LOREAL_ADDITIONAL_VISIBILITY]"
                                            + "[MID]" + mid + "[/MID]"
                                            + "[CREATED_BY]" + username + "[/CREATED_BY]"
                                            + "[BRAND_CD]" + additionalVisibilityData.get(j).getCompany_cd() + "[/BRAND_CD]"
                                            + "[DISPLAY_CD]" + additionalVisibilityData.get(j).getCategory_cd() + "[/DISPLAY_CD]"
                                           /* + "[CATEGORY_CD]" + marketintelligenceData.get(j).getPromotype_cd() + "[/CATEGORY_CD]"*/
                                            + "[PHOTO]" + additionalVisibilityData.get(j).getPhoto() + "[/PHOTO]"
                                            + "[REMARK]" + additionalVisibilityData.get(j).getRemark() + "[/REMARK]"
                                            + "[/LOREAL_ADDITIONAL_VISIBILITY]";

                                    paid_visibility = paid_visibility + onXML;
                                }
                                final String sos_xml = "[DATA]" + paid_visibility + "[/DATA]";
                                request = new SoapObject(CommonString.NAMESPACE, CommonString.METHOD_UPLOAD_XML);
                                request.addProperty("XMLDATA", sos_xml);
                                request.addProperty("KEYS", "LOREAL_ADDITIONAL_VISIBILITY");
                                request.addProperty("USERNAME", username);
                                request.addProperty("MID", mid);
                                envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
                                envelope.dotNet = true;
                                envelope.setOutputSoapObject(request);
                                androidHttpTransport = new HttpTransportSE(CommonString.URL);
                                androidHttpTransport.call(CommonString.SOAP_ACTION + CommonString.METHOD_UPLOAD_XML, envelope);
                                result = (Object) envelope.getResponse();
                                if (!result.toString().equalsIgnoreCase(CommonString.KEY_SUCCESS)) {
                                    isError = true;
                                }
                                data.value = 47;
                                data.name = "ADDITIONAL VISIBILITY Data";
                                publishProgress(data);

                            }

                            final_xml = "";
                            onXML = "";
                            paid_visibility = "";
                            sampledData = database.getinsertedsampledData(coverageBeanlist.get(i).getStoreId(), coverageBeanlist.get(i).getVisitDate());
                            if (sampledData.size() > 0) {
                                for (int j = 0; j < sampledData.size(); j++) {

                                    onXML = "[LOREAL_SAMPLED_DATA]"
                                            + "[MID]" + mid + "[/MID]"
                                            + "[CREATED_BY]" + username + "[/CREATED_BY]"
                                            + "[SKU_CD]" + sampledData.get(j).getSku_cd() + "[/SKU_CD]"
                                            + "[CATEGORY_CD]" + sampledData.get(j).getCategory_cd() + "[/CATEGORY_CD]"
                                            + "[SAMPLED]" + sampledData.get(j).getSampled() + "[/SAMPLED]"
                                            + "[PHOTO]" + sampledData.get(j).getSampled_img() + "[/PHOTO]"
                                            + "[FEEDBACK]" + sampledData.get(j).getFeedback() + "[/FEEDBACK]"
                                            + "[/LOREAL_SAMPLED_DATA]";

                                    paid_visibility = paid_visibility + onXML;
                                }
                                final String sos_xml = "[DATA]" + paid_visibility + "[/DATA]";
                                request = new SoapObject(CommonString.NAMESPACE, CommonString.METHOD_UPLOAD_XML);
                                request.addProperty("XMLDATA", sos_xml);
                                request.addProperty("KEYS", "LOREAL_SAMPLED_DATA");
                                request.addProperty("USERNAME", username);
                                request.addProperty("MID", mid);
                                envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
                                envelope.dotNet = true;
                                envelope.setOutputSoapObject(request);
                                androidHttpTransport = new HttpTransportSE(CommonString.URL);
                                androidHttpTransport.call(CommonString.SOAP_ACTION + CommonString.METHOD_UPLOAD_XML, envelope);
                                result = (Object) envelope.getResponse();
                                if (!result.toString().equalsIgnoreCase(CommonString.KEY_SUCCESS)) {
                                    isError = true;
                                }
                                data.value = 49;
                                data.name = "Market Intelligence Data";
                                publishProgress(data);

                            }


                            //Store Image
                            if (coverageBeanlist.size() > 0) {
                                if (coverageBeanlist.get(i).getImage() != null && !coverageBeanlist.get(i).getImage().equals("")) {
                                    if (new File(CommonString.FILE_PATH + coverageBeanlist.get(i).getImage()).exists()) {
                                        result = UploadImage(coverageBeanlist.get(i).getImage(), "StoreImages");
                                        if (!result.toString().equalsIgnoreCase(CommonString.KEY_SUCCESS)) {
                                            //    return "StoreImages";
                                            isError = true;
                                        }

                                        runOnUiThread(new Runnable() {
                                            public void run() {
                                                message.setText("Store Images Uploaded");
                                            }
                                        });
                                    }
                                }
                                data.value = 50;
                                data.name = "Store Images";
                                publishProgress(data);
                            }
                            //Stock Images
                            if (stockImages.size() > 0) {
                                for (int j = 0; j < stockImages.size(); j++) {

                                    if (stockImages.get(j).getImg_cam() != null && !stockImages.get(j).getImg_cam().equals("")) {
                                        if (new File(CommonString.FILE_PATH + stockImages.get(j).getImg_cam()).exists()) {
                                            result = UploadImage(stockImages.get(j).getImg_cam(), "StockImages");
                                            if (!result.toString().equalsIgnoreCase(CommonString.KEY_SUCCESS)) {
                                                //    return "StoreImages";
                                                isError = true;
                                            }
                                            runOnUiThread(new Runnable() {
                                                public void run() {
                                                    message.setText("Stock Images Uploaded");
                                                }
                                            });
                                        }
                                    }

                                    if (stockImages.get(j).getImg_cat_one() != null && !stockImages.get(j).getImg_cat_one().equals("")) {
                                        if (new File(CommonString.FILE_PATH + stockImages.get(j).getImg_cat_one()).exists()) {
                                            result = UploadImage(stockImages.get(j).getImg_cat_one(), "StockImages");
                                            if (!result.toString().equalsIgnoreCase(CommonString.KEY_SUCCESS)) {
                                                isError = true;
                                            }
                                            runOnUiThread(new Runnable() {
                                                public void run() {
                                                    message.setText("Stock Images Uploaded");
                                                }
                                            });
                                        }
                                    }
                                    if (stockImages.get(j).getImg_cat_two() != null && !stockImages.get(j).getImg_cat_two().equals("")) {
                                        if (new File(CommonString.FILE_PATH + stockImages.get(j).getImg_cat_two()).exists()) {
                                            result = UploadImage(stockImages.get(j).getImg_cat_two(), "StockImages");
                                            if (!result.toString().equalsIgnoreCase(CommonString.KEY_SUCCESS)) {
                                                isError = true;
                                            }
                                            runOnUiThread(new Runnable() {
                                                public void run() {
                                                    message.setText("Stock Images Uploaded");
                                                }
                                            });
                                        }
                                    }
                                }
                            }

                            data.value = 55;
                            data.name = "Stock Images";
                            publishProgress(data);


                            //Promotion Images
                            if (promotionData.size() > 0) {
                                for (int j = 0; j < promotionData.size(); j++) {
                                    if (promotionData.get(j).getCamera() != null && !promotionData.get(j).getCamera().equals("")) {
                                        if (new File(CommonString.FILE_PATH + promotionData.get(j).getCamera()).exists()) {
                                            result = UploadImage(promotionData.get(j).getCamera(), "PromotionImages");
                                            if (!result.toString().equalsIgnoreCase(CommonString.KEY_SUCCESS)) {
                                                isError = true;
                                            }

                                            runOnUiThread(new Runnable() {
                                                public void run() {
                                                    message.setText("Promotion Images Uploaded");
                                                }
                                            });
                                        }
                                    }
                                }
                                data.value = 60;
                                data.name = "PromotionImages";
                                publishProgress(data);
                            }


                            //Paid Visibility Images
                            if (paidVisibility.size() > 0) {
                                for (int j = 0; j < paidVisibility.size(); j++) {
                                    if (paidVisibility.get(j).getImg() != null && !paidVisibility.get(j).getImg().equals("")) {
                                        if (new File(CommonString.FILE_PATH + paidVisibility.get(j).getImg()).exists()) {
                                            result = UploadImage(paidVisibility.get(j).getImg(), "AssetImages");
                                            if (!result.toString().equalsIgnoreCase(CommonString.KEY_SUCCESS)) {
                                                //    return "StoreImages";
                                                isError = true;
                                            }

                                            runOnUiThread(new Runnable() {
                                                public void run() {
                                                    message.setText("Asset Images Uploaded");
                                                }
                                            });
                                        }
                                    }
                                }
                                data.value = 70;
                                data.name = "PaidVisibilityImage";
                                publishProgress(data);
                            }

                            if (additionalVisibilityData.size() > 0) {
                                for (int j = 0; j < additionalVisibilityData.size(); j++) {
                                    if (additionalVisibilityData.get(j).getPhoto() != null && !additionalVisibilityData.get(j).getPhoto().equals("")) {
                                        if (new File(CommonString.FILE_PATH + additionalVisibilityData.get(j).getPhoto()).exists()) {
                                            result = UploadImage(additionalVisibilityData.get(j).getPhoto(), "CompPromotionImages");
                                            if (!result.toString().equalsIgnoreCase(CommonString.KEY_SUCCESS)) {
                                                isError = true;
                                            }
                                            runOnUiThread(new Runnable() {
                                                public void run() {
                                                    message.setText("CompPromotion Images Uploaded");
                                                }
                                            });
                                        }
                                    }
                                }
                                data.value = 70;
                                data.name = "CompPromotion Images";
                                publishProgress(data);
                            }

                            if (sampledData.size() > 0) {
                                for (int j = 0; j < sampledData.size(); j++) {
                                    if (sampledData.get(j).getSampled_img() != null && !sampledData.get(j).getSampled_img().equals("")) {
                                        if (new File(CommonString.FILE_PATH + sampledData.get(j).getSampled_img()).exists()) {
                                            result = UploadImage(sampledData.get(j).getSampled_img(), "SamplingImages");
                                            if (!result.toString().equalsIgnoreCase(CommonString.KEY_SUCCESS)) {
                                                isError = true;
                                            }
                                            runOnUiThread(new Runnable() {
                                                public void run() {
                                                    message.setText("Sampling Images Uploaded");
                                                }
                                            });
                                        }
                                    }
                                }
                                data.value = 80;
                                data.name = "SamplingImages";
                                publishProgress(data);
                            }


                            // SET COVERAGE STATUS
                            String final_xml1 = "";
                            String onXML1 = "";
                            onXML1 = "[COVERAGE_STATUS]"
                                    + "[STORE_ID]" + coverageBeanlist.get(i).getStoreId() + "[/STORE_ID]"
                                    + "[VISIT_DATE]" + coverageBeanlist.get(i).getVisitDate() + "[/VISIT_DATE]"
                                    + "[USER_ID]" + coverageBeanlist.get(i).getUserId() + "[/USER_ID]"
                                    + "[STATUS]" + CommonString.KEY_U + "[/STATUS]"
                                    + "[/COVERAGE_STATUS]";

                            final_xml1 = final_xml1 + onXML1;

                            final String sos_xml = "[DATA]" + final_xml1 + "[/DATA]";

                            SoapObject request1 = new SoapObject(CommonString.NAMESPACE, CommonString.MEHTOD_UPLOAD_COVERAGE_STATUS);
                            request1.addProperty("onXML", sos_xml);
                            SoapSerializationEnvelope envelope1 = new SoapSerializationEnvelope(SoapEnvelope.VER11);
                            envelope1.dotNet = true;
                            envelope1.setOutputSoapObject(request1);

                            HttpTransportSE androidHttpTransport1 = new HttpTransportSE(CommonString.URL);
                            androidHttpTransport1.call(CommonString.SOAP_ACTION + CommonString.MEHTOD_UPLOAD_COVERAGE_STATUS, envelope1);
                            Object result1 = (Object) envelope1.getResponse();
                            if (result1.toString().equalsIgnoreCase(CommonString.KEY_SUCCESS)) {
                                database.open();
                                database.updateStoreStatusOnLeave(coverageBeanlist.get(i).getStoreId(), coverageBeanlist.get(i).getVisitDate(), CommonString.KEY_U);
                                database.updateCoverageStatus(coverageBeanlist.get(i).getMID(), CommonString.KEY_U);
                                database.deleteSpecificStoreData(coverageBeanlist.get(i).getStoreId());
                            }
                            if (!result1.toString().equalsIgnoreCase(CommonString.KEY_SUCCESS)) {
                                isError = true;
                            }
                            data.value = 100;
                            data.name = "Covrage Status";
                            publishProgress(data);
                            resultFinal = result.toString();
                        }
                    }
                }
            } catch (MalformedURLException e) {
                up_success_flag = false;
                exceptionMessage = e.toString();

            } catch (IOException e) {
                up_success_flag = false;
                exceptionMessage = e.toString();

            } catch (Exception e) {
                up_success_flag = false;
                exceptionMessage = e.toString();

            }

            if (up_success_flag == true) {
                return CommonString.KEY_SUCCESS;
            } else {
                return exceptionMessage;
            }
        }

        @Override
        protected void onProgressUpdate(Data... values) {
            pb.setProgress(values[0].value);
            percentage.setText(values[0].value + "%");
            message.setText(values[0].name);
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            dialog.dismiss();
            if (isError) {
                AlertDialog.Builder builder = new AlertDialog.Builder(UploadDataActivity.this);
                builder.setTitle("Parinaam");
                builder.setMessage("Uploaded successfully with some problem ").setCancelable(false)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                //temporary code
                                Intent i = new Intent(getBaseContext(), MainMenuActivity.class);
                                startActivity(i);
                                finish();
                            }
                        });
                AlertDialog alert = builder.create();
                alert.show();
            } else {
                if (result.equals(CommonString.KEY_SUCCESS)) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(UploadDataActivity.this);
                    builder.setTitle("Parinaam");
                    builder.setMessage(AlertMessage.MESSAGE_UPLOAD_DATA).setCancelable(false)
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    //temparory code
                                    Intent i = new Intent(getBaseContext(), MainMenuActivity.class);
                                    startActivity(i);
                                    finish();
                                }
                            });
                    AlertDialog alert = builder.create();
                    alert.show();
                } else if (result.equals(CommonString.KEY_FAILURE) || !result.equals("")) {
                    AlertMessage message = new AlertMessage(UploadDataActivity.this, CommonString.ERROR + result, "success", null);
                    message.showMessage();
                }
            }
        }
    }

    class Data {
        int value;
        String name;
    }

    @Override
    public void onBackPressed() {
        Intent i = new Intent(this, MainMenuActivity.class);
        startActivity(i);
        finish();
    }

    public String UploadImage(String path, String folder_path) throws Exception {
        errormsg = "";
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(Path + path, o);
        // The new size we want to scale to
        final int REQUIRED_SIZE = 1024;

        // Find the correct scale value. It should be the power of 2.
        int width_tmp = o.outWidth, height_tmp = o.outHeight;
        int scale = 1;
        while (true) {
            if (width_tmp < REQUIRED_SIZE && height_tmp < REQUIRED_SIZE)
                break;
            width_tmp /= 2;
            height_tmp /= 2;
            scale *= 2;
        }

        // Decode with inSampleSize
        BitmapFactory.Options o2 = new BitmapFactory.Options();
        o2.inSampleSize = scale;
        Bitmap bitmap = BitmapFactory.decodeFile(Path + path, o2);

        ByteArrayOutputStream bao = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, bao);
        byte[] ba = bao.toByteArray();
        String ba1 = Base64.encodeBytes(ba);
        SoapObject request = new SoapObject(CommonString.NAMESPACE, CommonString.METHOD_UPLOAD_IMAGE);
        String[] split = path.split("/");
        String path1 = split[split.length - 1];
        request.addProperty("img", ba1);
        request.addProperty("name", path1);
        request.addProperty("FolderName", folder_path);
        SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
        envelope.dotNet = true;
        envelope.setOutputSoapObject(request);
        HttpTransportSE androidHttpTransport = new HttpTransportSE(CommonString.URL);
        androidHttpTransport.call(CommonString.SOAP_ACTION_UPLOAD_IMAGE, envelope);

        Object result = envelope.getResponse();
        if (!result.toString().equalsIgnoreCase(CommonString.KEY_SUCCESS)) {
            if (result.toString().equalsIgnoreCase(CommonString.KEY_FALSE)) {
                return CommonString.KEY_FALSE;
            }
            SAXParserFactory saxPF = SAXParserFactory.newInstance();
            SAXParser saxP = saxPF.newSAXParser();
            XMLReader xmlR = saxP.getXMLReader();
            // for failure
            FailureXMLHandler failureXMLHandler = new FailureXMLHandler();
            xmlR.setContentHandler(failureXMLHandler);

            InputSource is = new InputSource();
            is.setCharacterStream(new StringReader(result.toString()));
            xmlR.parse(is);
            failureGetterSetter = failureXMLHandler.getFailureGetterSetter();
            if (failureGetterSetter.getStatus().equalsIgnoreCase(CommonString.KEY_FAILURE)) {
                errormsg = failureGetterSetter.getErrorMsg();
                return CommonString.KEY_FAILURE;
            }
        } else {
            new File(Path + path).delete();
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString(CommonString.KEY_STOREVISITED_STATUS, "");
            editor.commit();
        }

        return result.toString();
    }

    @Override
    protected void onStop() {
        super.onStop();
        database.close();
    }

}
