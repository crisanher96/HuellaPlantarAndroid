package com.huella_plantar.huellaplantar;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

public class ResultadosActivity extends AppCompatActivity {

    private String URL_PETICION= MainActivity.servidor+"/exa_insert.php";

    private ImageView imagen_izq, imagen_der;
    private double indice_der, indice_izq;
    private Button enviar;
    private EditText diagnostico;
    private ProgressBar progreso;

    private TextView tex_in_izq, tex_in_der, clasificacion_izq, clasificacion_der ,tex_vx_izq,tex_vy_izq,tex_vx_der,tex_vy_der;
    private String clas_izq, clas_der;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_resultados);
        enviar=findViewById(R.id.buttonenviar);
        progreso=findViewById(R.id.progress_resul);

        imagen_izq=findViewById(R.id.imageView3);
        imagen_der=findViewById(R.id.imageView4);
        tex_in_izq=findViewById(R.id.textView4);
        tex_in_der=findViewById(R.id.textView13);
        clasificacion_izq=findViewById(R.id.textView6);
        clasificacion_der=findViewById(R.id.textView15);

        tex_vx_izq=findViewById(R.id.textView8);
        tex_vy_izq=findViewById(R.id.textView10);
        tex_vx_der=findViewById(R.id.textView17);
        tex_vy_der=findViewById(R.id.textView19);

        diagnostico=findViewById(R.id.edit_diagnostico);
        if(MainActivity.conexion==1){
            diagnostico.setVisibility(View.GONE);
            enviar.setVisibility(View.GONE);
        }else {
            enviar.setVisibility(View.VISIBLE);
        }
        if(MainActivity.rol_enc.equals("2")){
            diagnostico.setVisibility(View.VISIBLE);
        }else{
            diagnostico.setVisibility(View.GONE);
        }
        indice_der=((ProcesarActivity.vx_der-ProcesarActivity.vy_der)/ProcesarActivity.vx_der)*100;
        indice_izq=((ProcesarActivity.vx_izq-ProcesarActivity.vy_izq)/ProcesarActivity.vx_izq)*100;

        indice_izq=Math.round(indice_izq);
        indice_der=Math.round(indice_der);
        if(indice_izq>=0 && indice_izq<35){
            clas_izq="Plano";
        }
        if(indice_izq>34 && indice_izq<40){
            clas_izq="Plano/Normal";
        }
        if(indice_izq>39 && indice_izq<55){
            clas_izq="Normal";
        }
        if(indice_izq>54 && indice_izq<60){
            clas_izq="Normal/Cavo";
        }
        if(indice_izq>59 && indice_izq<75){
            clas_izq="Cavo";
        }
        if(indice_izq>74 && indice_izq<85){
            clas_izq="Cavo Fuerte";
        }
        if(indice_izq>84 && indice_izq<101){
            clas_izq="Cavo Extremo";
        }

        if(indice_der>=0 && indice_der<35){
            clas_der="Plano";
        }
        if(indice_der>34 && indice_der<40){
            clas_der="Plano/Normal";
        }
        if(indice_der>39 && indice_der<55){
            clas_der="Normal";
        }
        if(indice_der>54 && indice_der<60){
            clas_der="Normal/Cavo";
        }
        if(indice_der>59 && indice_der<75){
            clas_der="Cavo";
        }
        if(indice_der>74 && indice_der<85){
            clas_der="Cavo Fuerte";
        }
        if(indice_der>84 && indice_der<101){
            clas_der="Cavo Extremo";
        }

        tex_in_izq.setText(String.format("%.0f",indice_izq)+" %");
        tex_in_der.setText(String.format("%.0f",indice_der)+" %");
        clasificacion_izq.setText(clas_izq);
        clasificacion_der.setText(clas_der);

        tex_vx_izq.setText(ProcesarActivity.vx_izq+"");
        tex_vy_izq.setText(ProcesarActivity.vy_izq+"");
        tex_vx_der.setText(ProcesarActivity.vx_der+"");
        tex_vy_der.setText(ProcesarActivity.vy_der+"");

        imagen_izq.setImageBitmap(ProcesarActivity.pieizqBitmap);
        imagen_der.setImageBitmap(ProcesarActivity.piederBitmap);

        enviar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Task1().execute();

            }
        });

    }

    class Task1 extends AsyncTask<Void,Void,Void>{
        @Override
        protected void onPreExecute() {
            progreso.setVisibility(View.VISIBLE);
            enviar.setEnabled(false);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            enviar_datos();
            return null;
        }
        @Override
        protected void onPostExecute(Void aVoid) {

        }
    }

    public void enviar_datos(){
        final String diagnostico=this.diagnostico.getText().toString();
        StringRequest stringRequest = new StringRequest(Request.Method.POST, URL_PETICION,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {

                        try {
                            JSONObject jsonObject = new JSONObject(response);
                            String success = jsonObject.getString( "success");
                            if(success.equals("1")){
                                Toast.makeText(ResultadosActivity.this, "Registro del examen exitoso.", Toast.LENGTH_SHORT).show();
                                progreso.setVisibility(View.GONE);
                                enviar.setEnabled(true);
                                Intent pant_paciente = new Intent(ResultadosActivity.this, paciente.class);
                                startActivity(pant_paciente);
                            }else{
                                progreso.setVisibility(View.GONE);
                                enviar.setEnabled(true);
                                Toast.makeText(ResultadosActivity.this, "Error en la conexión con el servidor.", Toast.LENGTH_SHORT).show();
                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                            progreso.setVisibility(View.GONE);
                            enviar.setEnabled(true);
                            Toast.makeText(ResultadosActivity.this, "Error del Sistema: "+ e.toString(), Toast.LENGTH_SHORT).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        progreso.setVisibility(View.GONE);
                        enviar.setEnabled(true);
                        Toast.makeText(ResultadosActivity.this, "Error del Sistema: "+ error.toString(), Toast.LENGTH_SHORT).show();
                    }
                })
        {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                String imagen_original=convertirImgString(paciente.rgbBitmap);
                String imagen_izq=convertirImgString(ProcesarActivity.pieizqBitmap);
                String imagen_der=convertirImgString(ProcesarActivity.piederBitmap);

                Map<String, String> parametros = new HashMap<>();
                parametros.put("id_paciente", paciente.id_paciente);
                parametros.put("id_enc_exa",MainActivity.id_enc);
                parametros.put("indice_izq",indice_izq+"");
                parametros.put("indice_der",indice_der+"");
                parametros.put("clasi_izq",clas_izq+"");
                parametros.put("clasi_der",clas_der+"");
                parametros.put("imagen_original",imagen_original);
                parametros.put("imagen_izq",imagen_izq);
                parametros.put("imagen_der",imagen_der);
                parametros.put("vx_izq",ProcesarActivity.vx_izq+"");
                parametros.put("vx_der",ProcesarActivity.vx_der+"");
                parametros.put("vy_izq",ProcesarActivity.vy_izq+"");
                parametros.put("vy_der",ProcesarActivity.vy_der+"");
                if(MainActivity.rol_enc.equals("2")){
                    parametros.put("estado_exa","1");
                    parametros.put("diagnostico",diagnostico);
                }else {
                    parametros.put("estado_exa","3");
                    parametros.put("diagnostico","");
                }

                return parametros;
            }
        };
        RequestQueue requestQueue= Volley.newRequestQueue( this);
        requestQueue.add(stringRequest);
    }

    private String convertirImgString(Bitmap bitmap) {
        ByteArrayOutputStream array=new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG,100,array);
        byte[] imagenByte=array.toByteArray();
        String imagenString=Base64.encodeToString(imagenByte,Base64.DEFAULT);
        return imagenString;
    }
}
