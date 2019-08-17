package com.huella_plantar.huellaplantar;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
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

import java.util.HashMap;
import java.util.Map;

public class RegistroActivity extends AppCompatActivity {

    private EditText num_iden, nombres, apellidos, email, contrasena, vcontrasena, telefono, direccion, fnacimiento;
    private Button registrar;
    private static String URL_PETICION="https://huellaplantar.webcindario.com/registro.php";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registro);

        num_iden=findViewById(R.id.editText1);
        nombres=findViewById(R.id.editText2);
        apellidos=findViewById(R.id.editText3);
        email=findViewById(R.id.editText4);
        contrasena=findViewById(R.id.editText5);
        vcontrasena=findViewById(R.id.editText6);
        telefono=findViewById(R.id.editText7);
        direccion=findViewById(R.id.editText8);
        fnacimiento=findViewById(R.id.editText9);

        registrar=findViewById(R.id.button1);


        registrar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (contrasena.getText().toString().equals(vcontrasena.getText().toString())){
                    registro();
                }else {
                    Toast.makeText(RegistroActivity.this, "Las contraseñas no coinciden.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }


    private void registro(){
        final String num_iden=this.num_iden.getText().toString().trim();
        final String nombres=this.nombres.getText().toString().trim();
        final String apellidos=this.apellidos.getText().toString().trim();
        final String email=this.email.getText().toString().trim();
        final String contrasena=this.contrasena.getText().toString().trim();
        final String telefono=this.telefono.getText().toString().trim();
        final String direccion=this.direccion.getText().toString().trim();
        final String fnacimiento=this.fnacimiento.getText().toString().trim();


        StringRequest stringRequest = new StringRequest(Request.Method.POST, URL_PETICION,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {

                        try {
                            JSONObject jsonObject = new JSONObject(response);
                            String success = jsonObject.getString( "success");



                            if(success.equals("1")){
                                Toast.makeText(RegistroActivity.this, "Registro exitoso", Toast.LENGTH_SHORT).show();
                                /*Intent pantcaptura = new Intent(RegistroActivity.this, CapturaActivity.class);
                                startActivity(pantcaptura);*/

                            }else{
                                Toast.makeText(RegistroActivity.this, "Problema de Registro", Toast.LENGTH_SHORT).show();
                            }


                        } catch (JSONException e) {
                            e.printStackTrace();
                            Toast.makeText(RegistroActivity.this, "Error del Sistema: "+ e.toString(), Toast.LENGTH_SHORT).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(RegistroActivity.this, "Error del Sistema: "+ error.toString(), Toast.LENGTH_SHORT).show();
                    }
                })
        {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> parametros = new HashMap<>();
                parametros.put("iden", num_iden);
                parametros.put("nombres", nombres);
                parametros.put("apellidos", apellidos);
                parametros.put("email", email);
                parametros.put("password", contrasena);
                parametros.put("telefono", telefono);
                parametros.put("direccion", direccion);
                parametros.put("fe_naci", fnacimiento);
                return parametros;
            }
        };

        RequestQueue requestQueue= Volley.newRequestQueue( this);
        requestQueue.add(stringRequest);
    }

}

