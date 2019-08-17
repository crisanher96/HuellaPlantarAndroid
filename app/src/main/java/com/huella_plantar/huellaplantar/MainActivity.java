package com.huella_plantar.huellaplantar;

import android.Manifest;
import android.app.ActivityManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
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


import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Map;


public class MainActivity extends AppCompatActivity {
    static String clave;
    static String servidor="https://huellaplantar.000webhostapp.com/android";
    //static String servidor="http://172.18.1.140:8000/android";
    static String rol_enc="0", id_enc="0";
    static int conexion=0;

    private EditText identificacion, contrasena;
    private Button ingresar, SinConexion;
    private ProgressBar progreso;
    private String URL_PETICION= servidor+"/login.php";

    private double total=0.0, libre=0.0, f_mayor=0.0;
    private int frecuencia=0, nucleos=0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        identificacion= findViewById(R.id.editText1);
        contrasena = findViewById(R.id.editText2);
        ingresar = findViewById(R.id.button1);
        SinConexion = findViewById(R.id.button2);
        progreso=findViewById(R.id.progress_main);
        conexion=0;

        VerificarPermisos();

        ingresar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                conexion=0;
                new Task1().execute();
            }
        });

        SinConexion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent pantpaciente = new Intent(MainActivity.this, paciente.class);
                startActivity(pantpaciente);
                conexion=1;
            }
        });

    }

    class Task1 extends AsyncTask<Void,Void,Void>{
        @Override
        protected void onPreExecute() {
            progreso.setVisibility(View.VISIBLE);
            ingresar.setEnabled(false);
            SinConexion.setEnabled(false);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            login();
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {

        }
    }


    private void VerificarPermisos (){
        int ReadPermission = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int RequestCode=0;
        if (ReadPermission != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},RequestCode);

        }
    }

    private void login(){
        final String identificacion=this.identificacion.getText().toString().trim();
        final String contrasena=this.contrasena.getText().toString().trim();

        StringRequest stringRequest = new StringRequest(Request.Method.POST, URL_PETICION,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {

                        try {
                            JSONObject jsonObject = new JSONObject(response);
                            String success = jsonObject.getString( "success");
                            String rol = jsonObject.getString( "rol");

                            if(success.equals("1")){
                                if (rol.equals("2") || rol.equals("3")){

                                    id_enc=jsonObject.getString( "identificacion");
                                    rol_enc=jsonObject.getString( "rol");

                                    Toast.makeText(MainActivity.this, "Login exitoso", Toast.LENGTH_SHORT).show();
                                    recursos();
                                    if(f_mayor>1.8){
                                        clave="podos1.0";
                                    }else{
                                        if(f_mayor==1.8){
                                            if(libre>=1796){
                                                clave="podos1.0";
                                            }else{
                                                clave="podos1.1";
                                            }
                                        }else {
                                            clave="podos1.1";
                                        }
                                    }
                                    progreso.setVisibility(View.GONE);
                                    ingresar.setEnabled(true);
                                    SinConexion.setEnabled(true);
                                    Intent pant_paciente = new Intent(MainActivity.this, paciente.class);
                                    startActivity(pant_paciente);
                                }else{
                                    Toast.makeText(MainActivity.this, "Su usuario no cuenta con permisos para acceder.", Toast.LENGTH_SHORT).show();
                                    progreso.setVisibility(View.GONE);
                                    ingresar.setEnabled(true);
                                    SinConexion.setEnabled(true);
                                }


                            } else if (success.equals("0")){
                                Toast.makeText(MainActivity.this, "Contraseña Incorrecta", Toast.LENGTH_SHORT).show();
                                progreso.setVisibility(View.GONE);
                                ingresar.setEnabled(true);
                                SinConexion.setEnabled(true);
                            }else{
                                Toast.makeText(MainActivity.this, "Documento no registrado", Toast.LENGTH_SHORT).show();
                                progreso.setVisibility(View.GONE);
                                ingresar.setEnabled(true);
                                SinConexion.setEnabled(true);
                            }


                        } catch (JSONException e) {
                            e.printStackTrace();
                            Toast.makeText(MainActivity.this, "Error del Sistema: "+ e.toString(), Toast.LENGTH_SHORT).show();
                            progreso.setVisibility(View.GONE);
                            ingresar.setEnabled(true);
                            SinConexion.setEnabled(true);
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(MainActivity.this, "Error del Sistema: "+ error.toString(), Toast.LENGTH_SHORT).show();
                        progreso.setVisibility(View.GONE);
                        ingresar.setEnabled(true);
                        SinConexion.setEnabled(true);
                    }
                })
        {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> parametros = new HashMap<>();
                parametros.put("iden", identificacion);
                parametros.put("password", contrasena);
                return parametros;
            }
        };

        RequestQueue requestQueue= Volley.newRequestQueue( this);
        requestQueue.add(stringRequest);

    }


    private void recursos(){
        ActivityManager.MemoryInfo memoria= new ActivityManager.MemoryInfo();
        ActivityManager  am= (ActivityManager)getSystemService(ACTIVITY_SERVICE);
        am.getMemoryInfo(memoria);
        libre=memoria.availMem/0x100000L;
        total=memoria.totalMem/0x100000L;
        nucleos=Runtime.getRuntime().availableProcessors();
        for (int i=0;i<nucleos;i++){
            frecuencia=getMaxCPUFreqMHz(i);
            if(frecuencia>f_mayor) {
                f_mayor = frecuencia;
            }
        }
        f_mayor=f_mayor/1000000.0;
    }

    public static int getMaxCPUFreqMHz(int nucleo) {

        int frecuencia;
        String cpuMaxFreq = "";
        RandomAccessFile reader = null;
        try {
            reader = new RandomAccessFile("/sys/devices/system/cpu/cpu"+nucleo+"/cpufreq/cpuinfo_max_freq", "r");
            cpuMaxFreq = reader.readLine();
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        frecuencia= Integer.parseInt(cpuMaxFreq);
        return frecuencia;
    }
}
