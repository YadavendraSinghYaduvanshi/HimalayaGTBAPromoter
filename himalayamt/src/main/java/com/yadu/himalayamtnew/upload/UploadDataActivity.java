package com.yadu.himalayamtnew.upload;

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
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.yadu.himalayamtnew.R;
import com.yadu.himalayamtnew.constants.AlertandMessages;
import com.yadu.himalayamtnew.constants.CommonString;
import com.yadu.himalayamtnew.database.HimalayaDb;
import com.yadu.himalayamtnew.delegates.CoverageBean;
import com.yadu.himalayamtnew.himalaya.MainMenuActivity;
import com.yadu.himalayamtnew.xmlGetterSetter.AssetInsertdataGetterSetter;
import com.yadu.himalayamtnew.xmlGetterSetter.Audit_QuestionDataGetterSetter;
import com.yadu.himalayamtnew.xmlGetterSetter.ChecklistInsertDataGetterSetter;
import com.yadu.himalayamtnew.xmlGetterSetter.FailureGetterSetter;
import com.yadu.himalayamtnew.xmlGetterSetter.JCPGetterSetter;
import com.yadu.himalayamtnew.xmlGetterSetter.PromotionInsertDataGetterSetter;
import com.yadu.himalayamtnew.xmlGetterSetter.SkuGetterSetter;
import com.yadu.himalayamtnew.xmlGetterSetter.StockNewGetterSetter;
import com.yadu.himalayamtnew.xmlHandler.FailureXMLHandler;

import org.json.JSONArray;
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

public class UploadDataActivity extends Activity {
    private Dialog dialog;
    private ProgressBar pb;
    private TextView percentage, message;
    String app_ver;
    private String visit_date, username;
    private SharedPreferences preferences;
    private HimalayaDb database;
    private int factor;
    String datacheck = "";
    String[] words;
    String validity;
    int mid;
    Data data;

    boolean upload_status;
    String Path;
    String errormsg = "";

    boolean isError = false;
    boolean up_success_flag = true;
    String exceptionMessage = "";
    String resultFinal;

    private ArrayList<CoverageBean> coverageBeanlist = new ArrayList<CoverageBean>();
    private FailureGetterSetter failureGetterSetter = null;
    private ArrayList<StockNewGetterSetter> stockData = new ArrayList<>();
    private ArrayList<StockNewGetterSetter> stockImages = new ArrayList<>();
    private ArrayList<PromotionInsertDataGetterSetter> promotionData = new ArrayList<>();
    private ArrayList<AssetInsertdataGetterSetter> paidVisibility = new ArrayList<>();
    private ArrayList<ChecklistInsertDataGetterSetter> paidVisibilityCheckList = new ArrayList<>();
    private ArrayList<SkuGetterSetter> sku_list = new ArrayList<>();
    ArrayList<Audit_QuestionDataGetterSetter> auditListData = new ArrayList<>();
    ArrayList<JCPGetterSetter> pjpDeviationStoreList;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_data);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        visit_date = preferences.getString(CommonString.KEY_DATE, null);
        username = preferences.getString(CommonString.KEY_USERNAME, null);
        app_ver = preferences.getString(CommonString.KEY_VERSION, "");
        database = new HimalayaDb(this);
        database.open();
        Intent i = getIntent();
        upload_status = i.getBooleanExtra("UploadAll", false);
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
                if (upload_status == false) {
                    coverageBeanlist = database.getCoverageData(visit_date);
                } else {
                    coverageBeanlist = database.getCoverageData(null);
                }

                if (coverageBeanlist.size() > 0) {
                    if (coverageBeanlist.size() == 1) {
                        factor = 50;
                    } else {
                        factor = 100 / (coverageBeanlist.size());
                    }
                }

                //PJP DEVIATION
                String final_xml = "";
                String onXML = "";
                pjpDeviationStoreList = database.getPJPDeviationStoreData(visit_date);
                if (pjpDeviationStoreList.size() > 0) {
                    for (int j = 0; j < pjpDeviationStoreList.size(); j++) {
                        onXML = "[JCP_DEVIATION]"
                                + "[CREATED_BY]" + username + "[/CREATED_BY]"
                                + "[STORE_CD]" + pjpDeviationStoreList.get(j).getStoreid().get(0) + "[/STORE_CD]"
                                + "[VISIT_DATE]" + pjpDeviationStoreList.get(j).getVisitdate().get(0) + "[/VISIT_DATE]"
                                + "[/JCP_DEVIATION]";

                        final_xml = final_xml + onXML;
                    }

                    final String sos_xml = "[DATA]" + final_xml + "[/DATA]";
                    SoapObject request = new SoapObject(CommonString.NAMESPACE, CommonString.METHOD_UPLOAD_XML);
                    request.addProperty("XMLDATA", sos_xml);
                    request.addProperty("KEYS", "PJP_DEVIATION");
                    request.addProperty("USERNAME", username);
                    request.addProperty("MID", "0");
                    SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
                    envelope.dotNet = true;
                    envelope.setOutputSoapObject(request);
                    HttpTransportSE androidHttpTransport = new HttpTransportSE(CommonString.URL);
                    androidHttpTransport.call(CommonString.SOAP_ACTION + CommonString.METHOD_UPLOAD_XML, envelope);
                    Object result = (Object) envelope.getResponse();
                    if (!result.toString().equalsIgnoreCase(CommonString.KEY_SUCCESS)) {
                        isError = true;
                    } else {
                        database.deletePJPDeviationStores();
                    }
                }



                data.value = 10;
                data.name = "Deviation JCP";
                publishProgress(data);

                for (int i = 0; i < coverageBeanlist.size(); i++) {
                    if (!coverageBeanlist.get(i).getStatus().equalsIgnoreCase(CommonString.KEY_U)) {
                        onXML =
                                "[DATA]"
                                        + "[USER_DATA]"
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
                            if (coverageBeanlist.get(i).isPJPDeviation()) {
                                database.updatePJPStoreStatus(coverageBeanlist.get(i).getStoreId(), coverageBeanlist.get(i).getVisitDate(), CommonString.KEY_P);
                            } else {
                                database.updateStoreStatusOnLeave(coverageBeanlist.get(i).getStoreId(), coverageBeanlist.get(i).getVisitDate(), CommonString.KEY_P);
                            }
                            database.updateCoverageStatus(coverageBeanlist.get(i).getMID(), CommonString.KEY_P);
                        } else {
                            isError = true;
                            continue;
                        }
                        mid = Integer.parseInt((words[1]));

                        data.value = 10;
                        data.name = "Uploading";
                        publishProgress(data);


                        //Stock Data
                        final_xml = "";
                        onXML = "";
                        stockData = database.getOpeningStockUpload(coverageBeanlist.get(i).getStoreId());

                        if (stockData.size() > 0) {
                            for (int j = 0; j < stockData.size(); j++) {
                                onXML = "[MT_STOCK_DATA]"
                                        + "[MID]" + mid + "[/MID]"
                                        + "[CREATED_BY]" + username + "[/CREATED_BY]"
                                        + "[SKU_CD]" + stockData.get(j).getSku_cd() + "[/SKU_CD]"
                                        + "[OPENING_STOCK]" + stockData.get(j).getEd_openingStock() + "[/OPENING_STOCK]"
                                        + "[OPENING_FACING]" + stockData.get(j).getEd_openingFacing() + "[/OPENING_FACING]"
                                        + "[MIDDAY_STOCK]" + stockData.get(j).getEd_midFacing() + "[/MIDDAY_STOCK]"
                                        + "[CLOSING_STOCK]" + stockData.get(j).getEd_closingFacing() + "[/CLOSING_STOCK]"
                                        //+ "[STOCK_UNDER_DAYS]" + stockData.get(j).getStock_under45days() + "[/STOCK_UNDER_DAYS]"
                                        + "[/MT_STOCK_DATA]";

                                final_xml = final_xml + onXML;
                            }

                            final String sos_xml = "[DATA]" + final_xml + "[/DATA]";

                            request = new SoapObject(CommonString.NAMESPACE, CommonString.METHOD_UPLOAD_XML);
                            request.addProperty("XMLDATA", sos_xml);
                            request.addProperty("KEYS", "MT_STOCK_DATA");
                            request.addProperty("USERNAME", username);
                            request.addProperty("MID", mid);

                            envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
                            envelope.dotNet = true;
                            envelope.setOutputSoapObject(request);

                            androidHttpTransport = new HttpTransportSE(CommonString.URL);
                            androidHttpTransport.call(CommonString.SOAP_ACTION + CommonString.METHOD_UPLOAD_XML, envelope);

                            result = (Object) envelope.getResponse();

                            if (!result.toString().equalsIgnoreCase(CommonString.KEY_SUCCESS)) {
                                //   return CommonString.METHOD_UPLOAD_XML;
                                isError = true;
                            }
                        }

                        data.value = 20;
                        data.name = "Stock Data";
                        publishProgress(data);


                        //Stock Image Data
                        final_xml = "";
                        onXML = "";
                        stockImages = database.getStockImageUploadData(coverageBeanlist.get(i).getStoreId());

                        if (stockImages.size() > 0) {
                            for (int j = 0; j < stockImages.size(); j++) {

                                if (!stockImages.get(j).getImg_cam().equals("") || !stockImages.get(j).getImg_cat_one().equals("")
                                        || !stockImages.get(j).getImg_cat_two().equals("")) {

                                    onXML = "[MT_STOCK_IMAGE_DATA]"
                                            + "[MID]" + mid + "[/MID]"
                                            + "[CREATED_BY]" + username + "[/CREATED_BY]"
                                            + "[CATEGORY_CD]" + stockImages.get(j).getCategory_cd() + "[/CATEGORY_CD]"
                                            + "[HIMALAYA_IMAGE]" + stockImages.get(j).getImg_cam() + "[/HIMALAYA_IMAGE]"
                                            + "[CATEGORY__ONE_IMAGE]" + stockImages.get(j).getImg_cat_one() + "[/CATEGORY__ONE_IMAGE]"
                                            + "[CATEGORY__TWO_IMAGE]" + stockImages.get(j).getImg_cat_two() + "[/CATEGORY__TWO_IMAGE]"
                                            + "[/MT_STOCK_IMAGE_DATA]";

                                    final_xml = final_xml + onXML;
                                }

                            }

                            final String sos_xml = "[DATA]" + final_xml + "[/DATA]";

                            request = new SoapObject(CommonString.NAMESPACE, CommonString.METHOD_UPLOAD_XML);
                            request.addProperty("XMLDATA", sos_xml);
                            request.addProperty("KEYS", "MT_STOCK_IMAGE_DATA_NEW");
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
                        }

                        data.value = 25;
                        data.name = "Stock Image Data";
                        publishProgress(data);


                        //Promotion Data
                        final_xml = "";
                        onXML = "";
                        promotionData = database.getPromotionUploadData(coverageBeanlist.get(i).getStoreId());

                        if (promotionData.size() > 0) {
                            for (int j = 0; j < promotionData.size(); j++) {
                                onXML = "[MT_PROMOTION_DATA]"
                                        + "[MID]" + mid + "[/MID]"
                                        + "[CREATED_BY]" + username + "[/CREATED_BY]"
                                        + "[PID]" + promotionData.get(j).getPid() + "[/PID]"
                                        + "[BRAND_CD]" + promotionData.get(j).getBrand_cd() + "[/BRAND_CD]"
                                        + "[CAMERA]" + promotionData.get(j).getCamera() + "[/CAMERA]"
                                        + "[PROMO_STOCK]" + promotionData.get(j).getPromoStock() + "[/PROMO_STOCK]"
                                        + "[PROMO_TALKER]" + promotionData.get(j).getPromoTalker() + "[/PROMO_TALKER]"
                                        + "[RUNNING_POS]" + promotionData.get(j).getRunningPOS() + "[/RUNNING_POS]"
                                        + "[/MT_PROMOTION_DATA]";

                                final_xml = final_xml + onXML;
                            }

                            final String sos_xml = "[DATA]" + final_xml + "[/DATA]";

                            request = new SoapObject(CommonString.NAMESPACE, CommonString.METHOD_UPLOAD_XML);
                            request.addProperty("XMLDATA", sos_xml);
                            request.addProperty("KEYS", "MT_PROMOTION_DATA");
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
                        }

                        data.value = 30;
                        data.name = "Promotion Data";
                        publishProgress(data);


                        //Paid Visibility Data
                        final_xml = "";
                        String checkList_xml = "", paid_visibility = "", skuList_xml = "";
                        onXML = "";

                        paidVisibility = database.getAssetUploadData(coverageBeanlist.get(i).getStoreId());

                        if (paidVisibility.size() > 0) {
                            for (int j = 0; j < paidVisibility.size(); j++) {

                                //CHECKLIST
                                paidVisibilityCheckList = database.getAssetCheckListUploadData(paidVisibility.get(j).getKey_id());
                                String checkList_list_xml = "", checkList = "";
                                if (paidVisibilityCheckList.size() > 0) {
                                    for (int c = 0; c < paidVisibilityCheckList.size(); c++) {
                                        checkList = "[CHECK_LIST_DATA]"
                                                + "[MID]" + mid + "[/MID]"
                                                + "[CREATED_BY]" + username + "[/CREATED_BY]"
                                                + "[COMMON_ID]" + paidVisibility.get(j).getKey_id() + "[/COMMON_ID]"
                                                + "[CHECK_LIST_ID]" + paidVisibilityCheckList.get(c).getChecklist_id() + "[/CHECK_LIST_ID]"
                                                + "[CHECK_LIST_TOGGLE]" + paidVisibilityCheckList.get(c).getChecklist_text() + "[/CHECK_LIST_TOGGLE]"
                                                + "[REASON_CD]" + paidVisibilityCheckList.get(c).getReason_cd() + "[/REASON_CD]"
                                                + "[/CHECK_LIST_DATA]";

                                        checkList_list_xml = checkList_list_xml + checkList;
                                    }
                                }

                                //skuList
                                //sku_list = database.getAssetSkuLisNewtData(paidVisibility.get(j).getKey_id());
                                String sku_list_xml = "", sku = "";
                                if (sku_list.size() > 0) {
                                    for (int s = 0; s < sku_list.size(); s++) {
                                        sku = "[SKU_LIST_DATA]"
                                                + "[MID]" + mid + "[/MID]"
                                                + "[CREATED_BY]" + username + "[/CREATED_BY]"
                                                + "[COMMON_ID]" + paidVisibility.get(j).getKey_id() + "[/COMMON_ID]"
                                                + "[SKU_CD]" + sku_list.get(s).getSKU_ID() + "[/SKU_CD]"
                                                + "[BRAND_CD]" + sku_list.get(s).getBRAND_ID() + "[/BRAND_CD]"
                                                + "[SKU_QUANTITY]" + sku_list.get(s).getQUANTITY() + "[/SKU_QUANTITY]"
                                                + "[/SKU_LIST_DATA]";

                                        sku_list_xml = sku_list_xml + sku;
                                    }
                                }


                                onXML = "[PAID_VISIBILITY]"
                                        + "[MID]" + mid + "[/MID]"
                                        + "[CREATED_BY]" + username + "[/CREATED_BY]"
                                        + "[CATEGORY_CD]" + paidVisibility.get(j).getCategory_cd() + "[/CATEGORY_CD]"
                                        + "[ASSET_CD]" + paidVisibility.get(j).getAsset_cd() + "[/ASSET_CD]"
                                        + "[COMMON_ID]" + paidVisibility.get(j).getKey_id() + "[/COMMON_ID]"
                                        + "[PRESENT]" + paidVisibility.get(j).getPresent() + "[/PRESENT]"
                                        + "[REMARK]" + paidVisibility.get(j).getRemark() + "[/REMARK]"
                                        + "[IMAGE]" + paidVisibility.get(j).getImg() + "[/IMAGE]"
                                        + "[CHECKLIST]" + checkList_list_xml + "[/CHECKLIST]"
                                        //+ "[SKULIST]" + sku_list_xml + "[/SKULIST]"
                                        + "[/PAID_VISIBILITY]";

                                paid_visibility = paid_visibility + onXML;
                            }


                            final String sos_xml = "[DATA]" + paid_visibility + "[/DATA]";

                            request = new SoapObject(CommonString.NAMESPACE, CommonString.METHOD_UPLOAD_XML);
                            request.addProperty("XMLDATA", sos_xml);
                            request.addProperty("KEYS", "MT_PAID_VISIBILITY_DATA_NEW");
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
                        }
                        data.value = 40;
                        data.name = "Paid Visibility Data";
                        publishProgress(data);

                        //Audit Data
                        final_xml = "";
                        onXML = "";

                        auditListData = database.getAfterSaveAuditQuestionAnswerData(coverageBeanlist.get(i).getStoreId(), "1");

                        if (auditListData.size() > 0) {
                            for (int j = 0; j < auditListData.size(); j++) {
                                onXML = "[MT_AUDIT_DATA]"
                                        + "[MID]" + mid + "[/MID]"
                                        + "[CREATED_BY]" + username + "[/CREATED_BY]"
                                        + "[QUESTION_ID]" + auditListData.get(j).getQuestion_id() + "[/QUESTION_ID]"
                                        + "[ANSWER_ID]" + auditListData.get(j).getSp_answer_id() + "[/ANSWER_ID]"
                                        + "[CATEGORY_ID]" + auditListData.get(j).getCategory_id() + "[/CATEGORY_ID]"
                                        + "[/MT_AUDIT_DATA]";

                                final_xml = final_xml + onXML;
                            }

                            final String sos_xml = "[DATA]" + final_xml + "[/DATA]";
                            request = new SoapObject(CommonString.NAMESPACE, CommonString.METHOD_UPLOAD_XML);
                            request.addProperty("XMLDATA", sos_xml);
                            request.addProperty("KEYS", "MT_AUDIT_DATA_NEW");
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
                        }

                        data.value = 45;
                        data.name = "Audit Data";
                        publishProgress(data);


                        //Images Upload
                        //Store Image
                        if (coverageBeanlist.size() > 0) {
                            if (coverageBeanlist.get(i).getImage() != null && !coverageBeanlist.get(i).getImage().equals("")) {
                                if (new File(CommonString.FILE_PATH + coverageBeanlist.get(i).getImage()).exists()) {
                                    result = UploadImage(coverageBeanlist.get(i).getImage(), "MTStoreImages");
                                    if (!result.toString().equalsIgnoreCase(CommonString.KEY_SUCCESS)) {
                                        isError = true;
                                    }
                                    runOnUiThread(new Runnable() {
                                        public void run() {
                                            message.setText("MTStoreImages Uploaded");
                                        }
                                    });
                                }
                            }
                        }
                        data.value = 50;
                        data.name = "StoreImages";
                        publishProgress(data);


                        //Stock Images
                        if (stockImages.size() > 0) {
                            for (int j = 0; j < stockImages.size(); j++) {

                                if (stockImages.get(j).getImg_cam() != null && !stockImages.get(j).getImg_cam().equals("")) {

                                    if (new File(CommonString.FILE_PATH + stockImages.get(j).getImg_cam()).exists()) {

                                        result = UploadImage(stockImages.get(j).getImg_cam(), "MTStockImages");

                                        if (!result.toString().equalsIgnoreCase(CommonString.KEY_SUCCESS)) {
                                            //    return "StoreImages";
                                            isError = true;
                                        }

                                        runOnUiThread(new Runnable() {
                                            public void run() {
                                                message.setText("MTStockImages Uploaded");
                                            }
                                        });
                                    }
                                }

                                if (stockImages.get(j).getImg_cat_one() != null && !stockImages.get(j).getImg_cat_one().equals("")) {

                                    if (new File(CommonString.FILE_PATH + stockImages.get(j).getImg_cat_one()).exists()) {

                                        result = UploadImage(stockImages.get(j).getImg_cat_one(), "MTStockImages");

                                        if (!result.toString().equalsIgnoreCase(CommonString.KEY_SUCCESS)) {
                                            isError = true;
                                        }

                                        runOnUiThread(new Runnable() {
                                            public void run() {
                                                message.setText("MTStockImages Uploaded");
                                            }
                                        });
                                    }
                                }

                                if (stockImages.get(j).getImg_cat_two() != null && !stockImages.get(j).getImg_cat_two().equals("")) {

                                    if (new File(CommonString.FILE_PATH + stockImages.get(j).getImg_cat_two()).exists()) {

                                        result = UploadImage(stockImages.get(j).getImg_cat_two(), "MTStockImages");

                                        if (!result.toString().equalsIgnoreCase(CommonString.KEY_SUCCESS)) {
                                            isError = true;
                                        }

                                        runOnUiThread(new Runnable() {
                                            public void run() {
                                                message.setText("MTStockImages Uploaded");
                                            }
                                        });
                                    }
                                }
                            }

                        }
                        data.value = 55;
                        data.name = "MTStockImages";
                        publishProgress(data);


                        //Promotion Images
                        if (promotionData.size() > 0) {
                            for (int j = 0; j < promotionData.size(); j++) {
                                if (promotionData.get(j).getCamera() != null && !promotionData.get(j).getCamera().equals("")) {

                                    if (new File(CommonString.FILE_PATH + promotionData.get(j).getCamera()).exists()) {

                                        result = UploadImage(promotionData.get(j).getCamera(), "MTPromotionImages");

                                        if (!result.toString().equalsIgnoreCase(CommonString.KEY_SUCCESS)) {
                                            isError = true;
                                        }

                                        runOnUiThread(new Runnable() {
                                            public void run() {
                                                message.setText("MTPromotionImages Uploaded");
                                            }
                                        });
                                    }
                                }
                            }

                        }
                        data.value = 60;
                        data.name = "PromotionImages";
                        publishProgress(data);


                        //Paid Visibility Images
                        if (paidVisibility.size() > 0) {

                            for (int j = 0; j < paidVisibility.size(); j++) {

                                if (paidVisibility.get(j).getImg() != null && !paidVisibility.get(j).getImg().equals("")) {

                                    if (new File(CommonString.FILE_PATH + paidVisibility.get(j).getImg()).exists()) {

                                        result = UploadImage(paidVisibility.get(j).getImg(), "MTPaidVisibilityImages");

                                        if (!result.toString().equalsIgnoreCase(CommonString.KEY_SUCCESS)) {
                                            isError = true;
                                        }

                                        runOnUiThread(new Runnable() {
                                            public void run() {
                                                message.setText("MTPaidVisibility Images Uploaded");
                                            }
                                        });
                                    }
                                }
                            }

                        }
                        data.value = 70;
                        data.name = "PaidVisibilityImage";
                        publishProgress(data);


                        data.value = factor * (i + 1);
                        data.name = "Uploading";
                        publishProgress(data);


                        // SET COVERAGE STATUS
                        String final_xml1 = "";
                        String onXML1 = "";
                        onXML1 = "[COVERAGE_STATUS]"
                                + "[STORE_ID]" + coverageBeanlist.get(i).getStoreId() + "[/STORE_ID]"
                                + "[VISIT_DATE]" + coverageBeanlist.get(i).getVisitDate() + "[/VISIT_DATE]"
                                + "[USER_ID]" + username + "[/USER_ID]"
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

                            if (coverageBeanlist.get(i).isPJPDeviation()) {
                                database.updatePJPStoreStatus(coverageBeanlist.get(i).getStoreId(),
                                        coverageBeanlist.get(i).getVisitDate(), CommonString.KEY_U);
                            } else {
                                database.updateStoreStatusOnLeave(coverageBeanlist.get(i).getStoreId(),
                                        coverageBeanlist.get(i).getVisitDate(), CommonString.KEY_U);
                            }

                            database.updateCoverageStatus(coverageBeanlist.get(i).getMID(), CommonString.KEY_U);


                            database.deleteSpecificStoreData(coverageBeanlist.get(i).getStoreId());
                        }

                        if (!result1.toString().equalsIgnoreCase(CommonString.KEY_SUCCESS)) {
                            isError = true;
                        }

                        data.value = 100;
                        publishProgress(data);
                        resultFinal = result.toString();
                    }
                }
            }
            catch (MalformedURLException e) {
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
                return resultFinal;
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
                    builder.setMessage(CommonString.MESSAGE_UPLOAD_DATA).setCancelable(false)
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
                    message(CommonString.ERROR + result);
                }
            }
        }
    }

    class Data {
        int value;
        String name;
    }

    private void message(String str) {
        android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(this);
        builder.setTitle("Parinaam").setMessage(str);
        builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                startActivity(new Intent(UploadDataActivity.this, MainMenuActivity.class));
                finish();
                dialog.dismiss();
            }
        });
        builder.show();
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