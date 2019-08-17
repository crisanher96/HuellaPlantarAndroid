package com.huella_plantar.huellaplantar;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class ProcesarActivity extends AppCompatActivity {

    static Bitmap pieizqBitmap, piederBitmap;
    static double vx_izq, vy_izq, vx_der, vy_der;

    public Scalar color_puntos=new Scalar(77, 255, 0);
    public Scalar color_lineas=new Scalar(255, 0, 0);
    public Scalar color_linfun=new Scalar(255, 68, 68);
    public Scalar color_texto=new Scalar(60, 60, 255);
    public Scalar color_lineaxy=new Scalar(100, 150, 0);
    public Scalar color_linsec=new Scalar(115, 115, 50);
    public int borPun=10, radPun=20, tamnoLetra=6, grosorLinea=7, grosorLetra=7;

    public Bitmap segBitmap;
    public Point unopder=new Point(), unoder=new Point(), dospder=new Point(), dosder=new Point(), x_der=new Point(), p1y_der=new Point(),p2y_der=new Point();
    public Point unoizq=new Point(), unopizq=new Point(), dosizq=new Point(), dospizq=new Point(), x_izq=new Point(), p1y_izq=new Point(),p2y_izq=new Point();
    public int Mfder=0, Mfizq=0, fx_der=0;
    public int anchoDer=0, anchoIzq=0;
    public double Vmfder=0, Vmfizq=0;



    private Button procesar, guardar;
    private EditText nombre;
    private ImageView imagen_ori, imagen_seg;
    private ProgressBar progreso;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        OpenCVLoader.initDebug();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_procesar);

        procesar=findViewById(R.id.buttonpro);
        imagen_ori=findViewById(R.id.imageView2);
        imagen_seg=findViewById(R.id.imageView5);
        progreso=findViewById(R.id.progressBar);
        guardar=findViewById(R.id.buttonguardar);
        nombre=findViewById(R.id.editTextNombre);
        imagen_ori.setImageBitmap(paciente.rgbBitmap);
        imagen_seg.setImageBitmap(paciente.segmentadaBitmap);

        if(paciente.segmentadaBitmap.getHeight()<650){
            borPun=5; radPun=10; tamnoLetra=3; grosorLinea=4; grosorLetra=4;
        }
        if(paciente.segmentadaBitmap.getHeight()>651 && paciente.segmentadaBitmap.getHeight()<1300){
            borPun=10; radPun=20; tamnoLetra=6; grosorLinea=7; grosorLetra=7;
        }
        if(paciente.segmentadaBitmap.getHeight()>1301 && paciente.segmentadaBitmap.getHeight()<2580){
            borPun=20; radPun=40; tamnoLetra=12; grosorLinea=14; grosorLetra=14;
        }

        procesar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Task1().execute();
                //segBitmap=paciente.segmentadaBitmap;
                //Proceso();

            }
        });

        guardar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String filename = nombre.getText().toString();
                imgwrite(filename,paciente.rgbBitmap,"/ImagenesCapturadas");

            }
        });


    }

    class Task1 extends AsyncTask<Void,Void,Void>{
        @Override
        protected void onPreExecute() {
            progreso.setVisibility(View.VISIBLE);
            procesar.setEnabled(false);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            //Ajuste y separacion de la imagen
            //segmentacion();
            segBitmap=paciente.segmentadaBitmap;
            Proceso();
            DibujarPuntos();
            DibujarLineas();

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            progreso.setVisibility(View.GONE);
            procesar.setEnabled(true);
            Intent pant_resultados = new Intent(ProcesarActivity.this, ResultadosActivity.class);
            startActivity(pant_resultados);
        }
    }

    public void imgwrite(String filename, Bitmap bmp, String ruta) {
        //--Proceso para asignar el nombre con la fecha y hora--//
        Calendar c = Calendar.getInstance();
        int dia = c.get(Calendar.DAY_OF_MONTH) + 1;
        int mes = c.get(Calendar.MONTH);
        int anio = c.get(Calendar.YEAR);
        int hora = c.get(Calendar.HOUR_OF_DAY);
        int minuto = c.get(Calendar.MINUTE);
        filename = filename+ "-" + dia + mes + anio + hora + minuto + ".jpg";
        //--Fin proceso para asignar el nombre con la fecha y hora--//
        FileOutputStream out = null;
        File sd = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + ruta);
        boolean success = true;
        if (!sd.exists()) {
            success = sd.mkdir();
        }
        if (success) {
            File dest = new File(sd, filename);

            try {
                out = new FileOutputStream(dest);
                bmp.compress(Bitmap.CompressFormat.JPEG, 100, out); // bmp is your Bitmap instance
                Toast.makeText(ProcesarActivity.this, "Imagen Guardada en Galeria", Toast.LENGTH_SHORT).show();
                // PNG is a lossless format, the compression factor (100) is ignored

            } catch (Exception e) {
                e.printStackTrace();
                //Log.d(TAG, e.getMessage());
            } finally {
                try {
                    if (out != null) {
                        out.close();
                        //      Log.d(TAG, "OK!!");
                    }
                } catch (IOException e) {
                    //Log.d(TAG, e.getMessage() + "Error");
                    e.printStackTrace();
                }
            }
        }
    }

    public void Proceso(){
        Mat segMat=new Mat();
        Mat imabinMat= new Mat();
        Utils.bitmapToMat(segBitmap,segMat);
        Imgproc.threshold(segMat,imabinMat,240,255,Imgproc.THRESH_BINARY);
        Rect recorteIzq = new Rect(0, 0, (int) imabinMat.cols()/2, (int)imabinMat.rows());
        Mat pieizqMat = new Mat(imabinMat, recorteIzq);
        Rect recorteDer = new Rect(imabinMat.cols()/2, 0, imabinMat.cols()-imabinMat.cols()/2, imabinMat.rows());
        Mat piederMat = new Mat(imabinMat, recorteDer);

        //Ancho del Pie
        anchoDer=ancho(piederMat);

        //Rotacion
        Point P1Der=punto1Der(piederMat);
        Point P1Izq=punto1Izq(pieizqMat);
        Point P1PDer=punto1PDer(piederMat);
        Point P1PIzq=punto1PIzq(pieizqMat);
        Mat PieDerG=rotacion (piederMat, P1Der, P1PDer);
        Mat PieIzqG=rotacion (pieizqMat, P1Izq, P1PIzq);
        //Fin Rotacion



        //Puntos 1 y 1p
        unoder=punto1Der(PieDerG);
        unoizq=punto1Izq(PieIzqG);
        unopder=punto1PDer(PieDerG);
        unopizq=punto1PIzq(PieIzqG);
        //Fin Puntos 1 y 1p



        //Puntos 2 y 2p
        dosder=punto2(PieDerG);
        dosizq=punto2(PieIzqG);
        dospder=punto2p(PieDerG);
        dospizq=punto2p(PieIzqG);
        //Fin Puntos 2 y 2p


        //Medida Fundamental
        MedidaFund();
        //Fin Medida Fundamental

        //Puntos X
        x_der=PuntoXDer(PieDerG,Mfder,(int) dosder.y);
        x_izq=PuntoXIzq(PieIzqG,Mfizq,(int) dosizq.y);
        //Fin Puntos X

        //Valor X
        vx_der=Math.abs(x_der.x-unoder.x);
        vx_izq=Math.abs(x_izq.x-unoizq.x);
        //Fin Valor X

        //Puntos Y1 y Y2
        p1y_der=Punto1Y (PieDerG, Mfder, (int) dosder.y);
        p1y_izq=Punto1Y (PieIzqG, Mfizq, (int) dosizq.y);
        p2y_der=Punto2Y (PieDerG, Mfder, (int) dosder.y);
        p2y_izq=Punto2Y (PieIzqG, Mfizq, (int) dosizq.y);
        //Fin Puntos Y1 y Y2

        //Valor Y
        vy_der=Math.abs(p1y_der.x-p2y_der.x);
        vy_izq=Math.abs(p1y_izq.x-p2y_izq.x);
        //Fin Valor Y

        pieizqBitmap=Bitmap.createBitmap(PieIzqG.width(), PieIzqG.height(), Bitmap.Config.RGB_565);
        Utils.matToBitmap(PieIzqG,pieizqBitmap);
        piederBitmap=Bitmap.createBitmap(PieDerG.width(), PieDerG.height(), Bitmap.Config.RGB_565);
        Utils.matToBitmap(PieDerG,piederBitmap);
    }
    public int ancho (Mat imagen){
        int valor=0;
        int c1=0,c2=0;
        int b_fin=0;
        for (int j=0;j<imagen.cols();j++) {
            for (int i = 0; i < (int) imagen.rows() / 2; i++) {
                double[] pixeles = imagen.get(i, j);
                if (pixeles[0] == 255) {
                    c1=j;
                    b_fin = 1;
                }
            }
            if (b_fin == 1) {
                break;
            }
        }
        for (int j=imagen.cols()-1;j>0;j--) {
            for (int i = 0; i < (int) imagen.rows() / 2; i++) {
                double[] pixeles = imagen.get(i, j);
                if (pixeles[0] == 255) {
                    c2=j;
                    b_fin = 1;
                }
            }
            if (b_fin == 1) {
                break;
            }
        }
        valor=Math.abs(c2-c1);
        valor=Math.round(valor*3/4);
        return valor;
    }
    public Point punto1Der (Mat imagen){
        Point p1 = new Point();
        int b_fin=0;
        int bandera=0;
        for (int j=0;j<imagen.cols();j++){
            for (int i=0;i<(int)imagen.rows()/2;i++){
                double[] pixeles=imagen.get(i, j);
                if(pixeles[0]==255){
                    for(int k=j;k<j+80;k++){
                        double[] pix2=imagen.get(i, k);
                        if(pix2[0]!=255){
                            bandera=1;
                            break;
                        }
                    }
                    if (bandera==0){
                        p1.x=j;
                        p1.y=i;
                        b_fin=1;
                        break;
                    }
                    bandera=0;
                }
            }
            if (b_fin==1){
                break;
            }
        }
        return p1;
    }
    public Point punto1PDer (Mat imagen){
        Point p1 = new Point();
        int b_fin=0;
        for (int j=0;j<imagen.cols();j++){
            for (int i=(int)imagen.rows()/2;i<imagen.rows();i++){
                double[] pixeles=imagen.get(i, j);
                if(pixeles[0]==255){
                    p1.x=j;
                    p1.y=i;
                    b_fin=1;
                    break;
                }
            }
            if (b_fin==1){
                break;
            }
        }
        return p1;
    }
    public Point punto1Izq (Mat imagen){
        Point p1 = new Point();
        int b_fin=0;
        int bandera=0;
        for (int j=imagen.cols()-1;j>0;j--){
            for (int i=0;i<(int)imagen.rows()/2;i++){
                double[] pixeles=imagen.get(i, j);
                if(pixeles[0]==255){
                    for(int k=j;k>j-80;k--){
                        double[] pix2=imagen.get(i, k);
                            if(pix2[0]!=255){
                                bandera=1;
                                break;
                            }
                    }
                    if(bandera==0){
                        p1.x=j;
                        p1.y=i;
                        b_fin=1;
                        break;
                    }
                    bandera=0;
                }
            }
            if (b_fin==1){
                break;
            }
        }
        return p1;
    }
    public Point punto1PIzq (Mat imagen){
        Point p1 = new Point();
        int b_fin=0;
        for (int j=imagen.cols()-1;j>0;j--){
            for (int i=(int)imagen.rows()/2;i<imagen.rows();i++){
                double[] pixeles=imagen.get(i, j);
                if(pixeles[0]==255){
                    p1.x=j;
                    p1.y=i;
                    b_fin=1;
                    break;
                }
            }
            if (b_fin==1){
                break;
            }
        }
        return p1;
    }
    public Mat rotacion (Mat imagen, Point P1, Point P1p){
        Mat Destino=new Mat();
        double m=0, angulo_radianes=0, angulo_grados=0;
        m=(P1p.y-P1.y)/(P1p.x-P1.x);
        angulo_radianes=Math.atan(m);
        angulo_grados=Math.toDegrees(angulo_radianes);
        if (angulo_grados>0){
            angulo_grados=angulo_grados-90;
        }else {
            angulo_grados=angulo_grados+90;
        }
        Mat r=Imgproc.getRotationMatrix2D(P1p,angulo_grados,1.0);
        Imgproc.warpAffine(imagen,Destino,r,imagen.size());
        return Destino;
    }
    public Point punto2(Mat imagen){
        Point P2 = new Point();
        int b_fin=0;
        for (int i=0;i<imagen.rows();i++){
            for (int j=0;j<imagen.cols();j++){
                double[] valor_pixel=imagen.get(i, j);
                if(valor_pixel[0]==255){
                    P2.x=j;
                    P2.y=i;
                    b_fin=1;
                    break;
                }
            }
            if(b_fin==1){
                break;
            }
        }
        return P2;
    }
    public Point punto2p(Mat imagen){
        Point P2p = new Point();
        int b_fin=0;
        for (int i=imagen.rows()-1;i>0;i--){
            for (int j=0;j<imagen.cols();j++){
                double[] valor_pixel=imagen.get(i, j);
                if(valor_pixel[0]==255){
                    P2p.x=j;
                    P2p.y=i;
                    b_fin=1;
                    break;
                }
            }
            if(b_fin==1){
                break;
            }
        }
        return P2p;
    }
    public void MedidaFund(){
        int DisTot=0;
        //Medida Fundamental Pie Derecho
        Mfder=(int) (unoder.y-dosder.y);
        DisTot=(int) (dospder.y-dosder.y);
        Vmfder=DisTot/Mfder;
        if (Math.round(Vmfder)>Vmfder){
            Vmfder=Math.round(Vmfder)-1;
        }else{
            Vmfder=Math.round(Vmfder);
        }
        //Medida Fundamental Pie Izquierdo
        Mfizq=(int) (unoizq.y-dosizq.y);
        DisTot=(int) (dospizq.y-dosizq.y);
        Vmfizq=DisTot/Mfizq;
        if (Math.round(Vmfizq)>Vmfizq){
            Vmfizq=Math.round(Vmfizq)-1;
        }else{
            Vmfizq=Math.round(Vmfizq);
        }
    }
    public void DibujarPuntos(){
        Mat PieDerMat = new Mat();
        Mat PieIzqMat= new Mat();
        Utils.bitmapToMat(piederBitmap,PieDerMat);
        Utils.bitmapToMat(pieizqBitmap,PieIzqMat);

        //Se Dibujan los puntos 1
        Imgproc.circle(PieDerMat,unoder,radPun,color_puntos,borPun);
        Imgproc.putText(PieDerMat,"1",unoder,1,tamnoLetra,color_texto,grosorLetra);
        Imgproc.circle(PieIzqMat,unoizq,radPun,color_puntos,borPun);
        Imgproc.putText(PieIzqMat,"1",unoizq,1,tamnoLetra,color_texto,grosorLetra);
        //Se Dibujan los puntos 1 primo
        Imgproc.circle(PieDerMat,unopder,radPun,color_puntos,borPun);
        Imgproc.putText(PieDerMat,"1'",unopder,1,tamnoLetra,color_texto,grosorLetra);
        Imgproc.circle(PieIzqMat,unopizq,radPun,color_puntos,borPun);
        Imgproc.putText(PieIzqMat,"1'",unopizq,1,tamnoLetra,color_texto,grosorLetra);
        //Se Dibujan los puntos 2
        Imgproc.circle(PieDerMat,dosder,radPun,color_puntos,borPun);
        Imgproc.putText(PieDerMat,"2",dosder,1,tamnoLetra,color_texto,grosorLetra);
        Imgproc.circle(PieIzqMat,dosizq,radPun,color_puntos,borPun);
        Imgproc.putText(PieIzqMat,"2",dosizq,1,tamnoLetra,color_texto,grosorLetra);
        //Se Dibujan los puntos 2p
        Imgproc.circle(PieDerMat,dospder,radPun,color_puntos,borPun);
        Imgproc.putText(PieDerMat,"2'",dospder,1,tamnoLetra,color_texto,grosorLetra);
        Imgproc.circle(PieIzqMat,dospizq,radPun,color_puntos,borPun);
        Imgproc.putText(PieIzqMat,"2'",dospizq,1,tamnoLetra,color_texto,grosorLetra);
        //Se Dibujan los puntos x
        Imgproc.circle(PieDerMat,x_der,radPun,color_puntos,borPun);
        Imgproc.circle(PieIzqMat,x_izq,radPun,color_puntos,borPun);
        //Se Dibujan los puntos Y
        Imgproc.circle(PieDerMat,p1y_der,radPun,color_puntos,borPun);
        Imgproc.circle(PieIzqMat,p1y_izq,radPun,color_puntos,borPun);
        Imgproc.circle(PieDerMat,p2y_der,radPun,color_puntos,borPun);
        Imgproc.circle(PieIzqMat,p2y_izq,radPun,color_puntos,borPun);

        Utils.matToBitmap(PieIzqMat,pieizqBitmap);
        Utils.matToBitmap(PieDerMat,piederBitmap);

    }
    public void DibujarLineas(){
        Mat PieDerMat = new Mat();
        Mat PieIzqMat= new Mat();
        Utils.bitmapToMat(piederBitmap,PieDerMat);
        Utils.bitmapToMat(pieizqBitmap,PieIzqMat);

        Point P1=new Point();
        Point P2=new Point();
        //Linea Principal Pie Derecho
        P1.y=1;
        P1.x=unoder.x;
        P2.y=PieDerMat.rows()-1;
        P2.x=unopder.x;
        Imgproc.line(PieDerMat,P1,P2,color_lineas,grosorLinea);
        //Linea Principal Pie Izquierdo
        P1.y=1;
        P1.x=unoizq.x;
        P2.y=PieIzqMat.rows()-1;
        P2.x=unopizq.x;
        Imgproc.line(PieIzqMat,P1,P2,color_lineas,grosorLinea);
        //Linea Superior Pie Derecho
        P1.x=1;
        P1.y=dosder.y;
        P2.x=PieDerMat.cols()-1;
        P2.y=dosder.y;
        Imgproc.line(PieDerMat,P1,P2,color_lineas,grosorLinea);
        //Linea Superior Pie Izquierdo
        P1.x=1;
        P1.y=dosizq.y;
        P2.x=PieIzqMat.cols()-1;
        P2.y=dosizq.y;
        Imgproc.line(PieIzqMat,P1,P2,color_lineas,grosorLinea);
        //Linea Inferior Pie Derecho
        P1.x=1;
        P1.y=dospder.y;
        P2.x=PieDerMat.cols()-1;
        P2.y=dospder.y;
        Imgproc.line(PieDerMat,P1,P2,color_lineas,grosorLinea);
        //Linea Inferior Pie Izquierdo
        P1.x=1;
        P1.y=dospizq.y;
        P2.x=PieIzqMat.cols()-1;
        P2.y=dospizq.y;
        Imgproc.line(PieIzqMat,P1,P2,color_lineas,grosorLinea);
        //Lineas Fundamentales Pie Derecho
        for(int i=1;i<=Vmfder;i++){
            P1.x=1;
            P1.y=i*Mfder+dosder.y;
            P2.x=PieDerMat.cols()-1;
            P2.y=i*Mfder+dosder.y;
            Imgproc.line(PieDerMat,P1,P2,color_lineas,grosorLinea);
        }
        //Lineas Fundamentales Pie Izquierdo
        for(int i=1;i<=Vmfizq;i++){
            P1.x=1;
            P1.y=i*Mfizq+dosizq.y;
            P2.x=PieIzqMat.cols()-1;
            P2.y=i*Mfizq+dosizq.y;
            Imgproc.line(PieIzqMat,P1,P2,color_lineas,grosorLinea);
        }
        //Lineas X
        Imgproc.line(PieDerMat,x_der,new Point((int)unoder.x,x_der.y),color_lineaxy,grosorLinea);
        Imgproc.putText(PieDerMat,"X",x_der,1,tamnoLetra,color_texto,grosorLetra);
        Imgproc.line(PieIzqMat,x_izq,new Point((int)unoizq.x,x_izq.y),color_lineaxy,grosorLinea);
        Imgproc.putText(PieIzqMat,"X",x_izq,1,tamnoLetra,color_texto,grosorLetra);
        //Lineas Y
        Imgproc.line(PieDerMat,p1y_der,p2y_der,color_lineaxy,grosorLinea);
        Imgproc.putText(PieDerMat,"Y",p1y_der,1,tamnoLetra,color_texto,grosorLetra);
        Imgproc.line(PieIzqMat,p1y_izq,p2y_izq,color_lineaxy,grosorLinea);
        Imgproc.putText(PieIzqMat,"Y",p1y_izq,1,tamnoLetra,color_texto,grosorLetra);
        //Lineas Verticales
        Imgproc.line(PieDerMat,p1y_der,new Point(p1y_der.x,Mfder*3+dosder.y),color_linsec,grosorLinea);
        Imgproc.line(PieDerMat,p2y_der,new Point(p2y_der.x,Mfder*3+dosder.y),color_linsec,grosorLinea);
        Imgproc.line(PieIzqMat,p1y_izq,new Point(p1y_izq.x,Mfizq*3+dosizq.y),color_linsec,grosorLinea);
        Imgproc.line(PieIzqMat,p2y_izq,new Point(p2y_izq.x,Mfizq*3+dosizq.y),color_linsec,grosorLinea);


        Utils.matToBitmap(PieIzqMat,pieizqBitmap);
        Utils.matToBitmap(PieDerMat,piederBitmap);

    }
    public Point PuntoXDer (Mat imagen, int Mf, int Fila2){
        Point Px = new Point();
        int b_fin=0;
        for (int j=imagen.cols()-1;j>0;j--){
            for (int i=Mf+Fila2;i<Mf*2+Fila2;i++){
                double[] valor_pixel=imagen.get(i, j);
                if(valor_pixel[0]==255){
                    Px.x=j;
                    Px.y=i;
                    b_fin=1;
                    break;
                }
            }
            if (b_fin==1){
                break;
            }
        }
        return Px;
    }
    public Point PuntoXIzq (Mat imagen, int Mf, int Fila2){
        Point Px = new Point();
        int b_fin=0;
        for (int j=0;j<imagen.cols();j++){
            for (int i=Mf+Fila2;i<Mf*2+Fila2;i++){
                double[] valor_pixel=imagen.get(i, j);
                if(valor_pixel[0]==255){
                    Px.x=j;
                    Px.y=i;
                    b_fin=1;
                    break;
                }
            }
            if (b_fin==1){
                break;
            }
        }
        return Px;
    }
    public Point Punto1Y (Mat imagen, int Mf, int Fila2){
        Point P1y=new Point();
        for (int j=0;j<imagen.cols();j++){
            double[] valor_pixel=imagen.get(Mf*2+Fila2, j);
            if(valor_pixel[0]==255){
                P1y.x=j;
                P1y.y=Mf*2+Fila2;
                break;
            }
        }
        return P1y;
    }
    public Point Punto2Y (Mat imagen, int Mf, int Fila2){
        Point P2y=new Point();
        for (int j=imagen.cols()-1;j>0;j--){
            double[] valor_pixel=imagen.get(Mf*2+Fila2, j);
            if(valor_pixel[0]==255){
                P2y.x=j;
                P2y.y=Mf*2+Fila2;
                break;
            }
        }
        return P2y;
    }
}
