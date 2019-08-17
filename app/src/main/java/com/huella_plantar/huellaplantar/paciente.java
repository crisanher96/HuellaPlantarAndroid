package com.huella_plantar.huellaplantar;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class paciente extends AppCompatActivity {

    static String id_paciente;
    static Bitmap rgbBitmap, segmentadaBitmap;

    private Uri imageUri;

    private Button buscar, capturar, galeria;
    private EditText iden_paciente;
    private TextView identificacion, nombre, titulo, TituloPrincipal;
    private ProgressBar progresosoc;

    private String URL_PETICION=MainActivity.servidor+"/buscar_paciente.php";

    //Variables para el socket
    static Socket clienteSocket;
    static int puerto=9000;
    static String ip="10.42.0.1";
    //static String ip="192.168.0.28";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        OpenCVLoader.initDebug();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_paciente);

        buscar=findViewById(R.id.button1);
        capturar=findViewById(R.id.button2);
        galeria=findViewById(R.id.button3);
        iden_paciente=findViewById(R.id.editText);
        identificacion=findViewById(R.id.textView2);
        nombre=findViewById(R.id.textView3);
        progresosoc=findViewById(R.id.progresosocket);
        titulo=findViewById(R.id.text3);
        TituloPrincipal=findViewById(R.id.text1);

        if(MainActivity.conexion==1){
            TituloPrincipal.setText("Escoja su Opción");
            iden_paciente.setVisibility(View.GONE);
            buscar.setVisibility(View.GONE);
            capturar.setVisibility(View.VISIBLE);
            galeria.setVisibility(View.VISIBLE);
        }else{
            TituloPrincipal.setText("Ingrese el Documento del Paciente");
            iden_paciente.setVisibility(View.VISIBLE);
            buscar.setVisibility(View.VISIBLE);
            capturar.setVisibility(View.GONE);
            galeria.setVisibility(View.GONE);
        }

        buscar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new TaskBuscar().execute();
            }
        });

        capturar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rgbBitmap=null;
                new TaskSocket().execute();

            }
        });

        galeria.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rgbBitmap=null;

                openGallery();

            }
        });
    }

   public void openGallery(){
        Intent myIntent=new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(myIntent,100);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==100 && resultCode==RESULT_OK && data!=null)
        {
            imageUri=data.getData();
            try
            {
                rgbBitmap=MediaStore.Images.Media.getBitmap(this.getContentResolver(),imageUri);
                segmentacion();
                Intent pant_procesar = new Intent(paciente.this, ProcesarActivity.class);
                startActivity(pant_procesar);
            }catch (IOException e){
                e.printStackTrace();
            }
        }
    }//Con la apertura de la galeria

    public void buscar_paciente(){
        final String iden=this.iden_paciente.getText().toString().trim();

        StringRequest stringRequest = new StringRequest(Request.Method.POST, URL_PETICION,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {

                        try {
                            JSONObject jsonObject = new JSONObject(response);
                            String success = jsonObject.getString( "success");
                            if(success.equals("1")){
                                identificacion.setVisibility(View.VISIBLE);
                                nombre.setVisibility(View.VISIBLE);
                                capturar.setVisibility(View.VISIBLE);
                                galeria.setVisibility(View.VISIBLE);
                                titulo.setVisibility(View.VISIBLE);
                                identificacion.setText("Identificación: "+iden);
                                nombre.setText("Nombre: "+jsonObject.getString( "nombre"));
                                id_paciente=jsonObject.getString( "identificacion");

                            }else {
                                identificacion.setVisibility(View.GONE);
                                nombre.setVisibility(View.GONE);
                                capturar.setVisibility(View.GONE);
                                galeria.setVisibility(View.GONE);
                                titulo.setVisibility(View.GONE);
                                Toast.makeText(paciente.this, "Paciente no registrado.", Toast.LENGTH_SHORT).show();
                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                            Toast.makeText(paciente.this, "Error del Sistema: "+ e.toString(), Toast.LENGTH_SHORT).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse( VolleyError error) {
                        Toast.makeText(paciente.this, "Error del Sistema: "+ error.toString(), Toast.LENGTH_SHORT).show();
                    }
                })
        {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> parametros = new HashMap<>();
                parametros.put("iden", iden);
                return parametros;
            }
        };

        RequestQueue requestQueue= Volley.newRequestQueue( this);
        requestQueue.add(stringRequest);
    }

    public void segmentacion(){
        Mat rgbMat=new Mat();  //Mat con la imagen original
        Mat grayMat=new Mat(); //Mat con la imagen en escala de grises
        Mat segMat_temp=new Mat();  //Mat con la imagen segmentada con basura
        Mat contMat=new Mat(); //Mat con la segmentacion final
        Mat imabin=new Mat(); //Mat con la imagen en binario

        int width =  paciente.rgbBitmap.getWidth(); //Numero de filas de la imagen
        int height =  paciente.rgbBitmap.getHeight(); //Numero de columnas de la imagen
        double area; //Area de los contornos
        double max_area; //Area del contorno mas grande dentro de la imagen

        Bitmap contBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565); //Bitmap negro para dibujar los contornos
        segmentadaBitmap=contBitmap;
        Utils.bitmapToMat( paciente.rgbBitmap, rgbMat); //Conversion de la imagen a Mat
        Utils.bitmapToMat(contBitmap, contMat);// Conversion del fondo negro a Mat

        //--Proceso de segmentacion temporal con basura--//
        Imgproc.cvtColor(rgbMat, grayMat, Imgproc.COLOR_RGB2GRAY); //Conversion de la imagen a escala de grises
        Imgproc.threshold(grayMat, segMat_temp, 0,255, Imgproc.THRESH_OTSU); //Segmentacion de la imagen por otsu
        //--Fin del proceso temporal de segmentacion--//

        //--Proceso de eliminacion de basura con contornos--//
        final List<MatOfPoint> points = new ArrayList<>();
        final Mat hierarchy = new Mat();

        Imgproc.findContours(segMat_temp, points, hierarchy,Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);
        max_area=0;
        for (int contourIdx = 0; contourIdx < points.size(); contourIdx++) {
            area=Imgproc.contourArea(points.get(contourIdx));
            if(area>=max_area) {
                max_area=area;
            }
        }
        max_area=max_area/100;

        for (int contourIdx = 0; contourIdx < points.size(); contourIdx++) {
            area = Imgproc.contourArea(points.get(contourIdx));
            if(area>=max_area) {
                //Toast.makeText(CapturaActivity.this, "Umbral Area: "+max_area, Toast.LENGTH_SHORT).show();
                Imgproc.drawContours(contMat, points, contourIdx, new Scalar(255, 255, 255), -1);
            }
        }
        Imgproc.cvtColor(contMat, grayMat, Imgproc.COLOR_RGB2GRAY);
        Imgproc.threshold(grayMat,imabin,100,255,Imgproc.THRESH_BINARY);
        //--Fin del proceso de eliminacion de basura con contornos--//
        Utils.matToBitmap(imabin,segmentadaBitmap);
    } //Realiza segmentacion de la imagen

    class TaskSocket extends AsyncTask<Void,Void,Void>{
        Mat tempMat=new Mat();
        Mat rgbMat=new Mat();

        @Override
        protected void onPreExecute() {
            buscar.setEnabled(false);
            capturar.setEnabled(false);
            galeria.setEnabled(false);
            progresosoc.setVisibility(View.VISIBLE);

        }

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                clienteSocket=new Socket(ip,puerto);
                ObjectInputStream ois = new ObjectInputStream(clienteSocket.getInputStream());
                byte [] buffer = (byte[]) ois.readObject();
                ByteArrayInputStream arrayimagen = new ByteArrayInputStream(buffer);
                rgbBitmap=BitmapFactory.decodeStream(arrayimagen);
                clienteSocket.close();
                if(paciente.rgbBitmap.getWidth()>=900){
                    Utils.bitmapToMat(paciente.rgbBitmap, tempMat);
                    Imgproc.resize(tempMat, rgbMat, new Size(879,643));
                    rgbBitmap=Bitmap.createBitmap(879, 643, Bitmap.Config.RGB_565);
                    Utils.matToBitmap(rgbMat,rgbBitmap);
                }
                segmentacion();
            }catch (Exception e){
                //error=e.toString();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            //Toast.makeText(MainActivity.this, "Proceso Finalizado Correctamente", Toast.LENGTH_SHORT).show();
            //Toast.makeText(MainActivity.this, error, Toast.LENGTH_SHORT).show();
            progresosoc.setVisibility(View.GONE);
            buscar.setEnabled(true);
            capturar.setEnabled(true);
            galeria.setEnabled(true);
            Intent pant_procesar = new Intent(paciente.this, ProcesarActivity.class);
            startActivity(pant_procesar);
        }
    }

    class TaskBuscar extends AsyncTask<Void,Void,Void>{
        @Override
        protected void onPreExecute() {
            progresosoc.setVisibility(View.VISIBLE);
            buscar.setEnabled(false);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            buscar_paciente();
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            progresosoc.setVisibility(View.GONE);
            buscar.setEnabled(true);
        }
    }


}
