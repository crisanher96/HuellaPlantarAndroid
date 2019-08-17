package com.huella_plantar.huellaplantar;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.*;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;


public class CapturaActivity extends AppCompatActivity {


    //Vaiables Inciiales
    private TextView texto1;
    private EditText identificacion;
    private Button capturar, procesar;
    private ImageView imageIzq, imageDer;
    private Uri imageUri;
    public Scalar color_puntos=new Scalar(77, 255, 0);
    public Scalar color_lineas=new Scalar(255, 0, 0);
    public Scalar color_linfun=new Scalar(255, 68, 68);
    public Scalar color_texto=new Scalar(60, 60, 255);
    public Scalar color_lineaxy=new Scalar(100, 150, 0);
    public Scalar color_linsec=new Scalar(115, 115, 50);
    public int borPun=3, radPun=7;

    public Bitmap rgbBitmap, segBitmap, pieizqBitmap, piederBitmap;
    public Point unopder=new Point(), unoder=new Point(), dospder=new Point(), dosder=new Point(), x_der=new Point(), p1y_der=new Point(),p2y_der=new Point();
    public Point unoizq=new Point(), unopizq=new Point(), dosizq=new Point(), dospizq=new Point(), x_izq=new Point(), p1y_izq=new Point(),p2y_izq=new Point();
    public int Mfder=0, Mfizq=0, fx_der=0;
    public double Vmfder=0, Vmfizq=0;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        OpenCVLoader.initDebug();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_captura);
        //Codigo Inicial
            identificacion = findViewById(R.id.editText1);
            capturar = findViewById(R.id.button1);
            procesar = findViewById(R.id.button);
            imageIzq = findViewById(R.id.imageIzq);
            imageDer = findViewById(R.id.imageDer);
            texto1=findViewById(R.id.text1);

            texto1.setText(MainActivity.clave+" "+MainActivity.id_enc+" "+MainActivity.rol_enc);

        //Metodos de los botones
        capturar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openGallery(); //Se carga la imagen
            }
        });
        procesar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Ajuste y separacion de la imagen
                segmentacion();
                separarpies();
                //Proceso en el pie derecho
                puntos1y1p(0);
                puntos2y2p(0);
                medfund(0);
                valorx(0);
                valory(0);
                dibujar_lineas(0);
                dibujar_puntos(0);

                //Proceso en el pie Izquierdo
                espejo(1);
                puntos1y1p(1);
                puntos2y2p(1);
                medfund(1);
                valorx(1);
                valory(1);
                ajuste_punto(1);
                espejo(1);
                dibujar_lineas(1);
                dibujar_puntos(1);

                //Proceso de visualizacion
                imshow();
                procesar.setVisibility(View.GONE);
            }
        });
    }

    //Meotodos Creados
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
            }catch (IOException e){
                e.printStackTrace();
            }
        }
        procesar.setVisibility(View.VISIBLE);
        imageIzq.setVisibility(View.VISIBLE);
        imageDer.setVisibility(View.GONE);
        imageIzq.setImageBitmap(rgbBitmap);
    }

    public void segmentacion(){
        Mat rgbMat=new Mat();  //Mat con la imagen original
        Mat grayMat=new Mat(); //Mat con la imagen en escala de grises
        Mat segMat_temp=new Mat();  //Mat con la imagen segmentada con basura
        Mat contMat=new Mat(); //Mat con la segmentacion final
        Mat imabin=new Mat(); //Mat con la imagen en binario

        int width = rgbBitmap.getWidth(); //Numero de filas de la imagen
        int height = rgbBitmap.getHeight(); //Numero de columnas de la imagen
        double area; //Area de los contornos
        double max_area; //Area del contorno mas grande dentro de la imagen

        Bitmap contBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565); //Bitmap negro para dibujar los contornos
        segBitmap=contBitmap;
        Utils.bitmapToMat(rgbBitmap, rgbMat); //Conversion de la imagen a Mat
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
        max_area=max_area/120;

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
        Utils.matToBitmap(imabin,segBitmap);
    } //Realiza segmentacion de la imagen

    public void imgwrite(String filename, Bitmap bmp, String ruta){
        //--Proceso para asignar el nombre con la fecha y hora--//
        Calendar c = Calendar.getInstance();
        int dia=c.get(Calendar.DAY_OF_MONTH)+1;
        int mes=c.get(Calendar.MONTH);
        int anio=c.get(Calendar.YEAR);
        int hora=c.get(Calendar.HOUR_OF_DAY);
        int minuto=c.get(Calendar.MINUTE);
        int segundo=c.get(Calendar.SECOND);
        filename=filename+dia+mes+anio+hora+minuto+segundo+".jpg";
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

    } //Guarda un bitmap en la ruta expuesta

    public void imshow(){
        imageIzq.setVisibility(View.VISIBLE);
        imageIzq.setImageBitmap(pieizqBitmap);
        imageDer.setVisibility(View.VISIBLE);
        imageDer.setImageBitmap(piederBitmap);
    } //Muestra las imagenes en los imageview

    public void separarpies(){
        Mat segMat=new Mat();
        Mat imabinMat= new Mat();
        Utils.bitmapToMat(segBitmap,segMat);
        Imgproc.threshold(segMat,imabinMat,240,255,Imgproc.THRESH_BINARY);
        Size tamanio = segMat.size();
        int filas=(int) tamanio.height;
        int margen=30;
        int columnas=(int) tamanio.width;
        int b_fin=0;
        Point pcentro=new Point(Math.round(columnas/2),Math.round(filas/2));
        Point pizqpri=new Point(), pizqseg=new Point(), pizqarr=new Point(), pizqaba=new Point();
        Point pderpri=new Point(), pderseg=new Point(), pderarr=new Point(), pderaba=new Point();
        //--Proceso para encontrar los limites del pie Izquierdo--//
        for (int i=0;i<=pcentro.x;i++){
            for (int j=filas-1;j>=0;j--){
                double[] pixeles=imabinMat.get(j, i);
                if(pixeles[0]==255){
                    pizqpri.x=i;
                    pizqpri.y=j;
                    b_fin=1;
                    break;
                }
            }
            if (b_fin==1){
                break;
            }
        }
        b_fin=0;
        for (int i=(int) pcentro.x;i>=0;i--){
            for (int j=filas-1;j>=0;j--){
                double[] pixeles=imabinMat.get(j, i);
                if(pixeles[0]==255){
                    pizqseg.x=i;
                    pizqseg.y=j;
                    b_fin=1;
                    break;
                }
            }
            if (b_fin==1){
                break;
            }
        }
        b_fin=0;
        for (int i=0;i<=pcentro.y;i++){
            for (int j=0;j<=pcentro.x;j++){
                double[] pixeles=imabinMat.get(i, j);
                if(pixeles[0]==255){
                    pizqarr.x=j;
                    pizqarr.y=i;
                    b_fin=1;
                    break;
                }
            }
            if (b_fin==1){
                break;
            }
        }
        b_fin=0;
        for (int i=filas-1;i>=pcentro.y;i--){
            for (int j=0;j<=pcentro.x;j++){
                double[] pixeles=imabinMat.get(i, j);
                if(pixeles[0]==255){
                    pizqaba.x=j;
                    pizqaba.y=i;
                    b_fin=1;
                    break;
                }
            }
            if (b_fin==1){
                break;
            }
        }
        //--Fin proceso para encontrar los limites del pie Izquierdo--//
        //--Proceso para encontrar los limites del pie Derecho--//
        b_fin=0;
        for (int i=(int) pcentro.x;i<=columnas-1;i++){
            for (int j=filas-1;j>=0;j--){
                double[] pixeles=imabinMat.get(j, i);
                if(pixeles[0]==255){
                    pderpri.x=i;
                    pderpri.y=j;
                    b_fin=1;
                    break;
                }
            }
            if (b_fin==1){
                break;
            }
        }
        b_fin=0;
        for (int i=columnas-1;i>=pcentro.x;i--){
            for (int j=filas-1;j>=0;j--){
                double[] pixeles=imabinMat.get(j, i);
                if(pixeles[0]==255){
                    pderseg.x=i;
                    pderseg.y=j;
                    b_fin=1;
                    break;
                }
            }
            if (b_fin==1){
                break;
            }
        }
        b_fin=0;
        for (int i=0;i<=pcentro.y;i++){
            for (int j=(int)pcentro.x;j<=columnas-1;j++){
                double[] pixeles=imabinMat.get(i, j);
                if(pixeles[0]==255){
                    pderarr.x=j;
                    pderarr.y=i;
                    b_fin=1;
                    break;
                }
            }
            if (b_fin==1){
                break;
            }
        }
        b_fin=0;
        for (int i=filas-1;i>=pcentro.y;i--){
            for (int j=(int)pcentro.x;j<=columnas-1;j++){
                double[] pixeles=imabinMat.get(i, j);
                if(pixeles[0]==255){
                    pderaba.x=j;
                    pderaba.y=i;
                    b_fin=1;
                    break;
                }
            }
            if (b_fin==1){
                break;
            }
        }
        //--Fin proceso para encontrar los limites del pie Derecho--//
        //--Proceso separacion de los pies en cada bitmap--//
        //Pie Izquierdo
        int col_pizq=((int) pizqseg.x- (int) pizqpri.x)+margen*2;
        int fil_pizq=filas;
        Rect rectizq=new Rect((int)pizqpri.x-margen,0,col_pizq,fil_pizq);
        Mat pieizqMat = new Mat(imabinMat, rectizq);
        pieizqBitmap=Bitmap.createBitmap(pieizqMat.width(), pieizqMat.height(), Bitmap.Config.RGB_565);
        Utils.matToBitmap(pieizqMat,pieizqBitmap);
        //Pie Derecho
        int col_pider=((int) pderseg.x- (int) pderpri.x)+margen*2;
        int fil_pider=filas;
        Rect rect1=new Rect((int)pderpri.x-margen,0,col_pider,fil_pider);
        Mat piederMat = new Mat(imabinMat, rect1);
        piederBitmap=Bitmap.createBitmap(piederMat.width(), piederMat.height(), Bitmap.Config.RGB_565);
        Utils.matToBitmap(piederMat,piederBitmap);
        //--Fin proceso separacion de los pies en cada bitmap--//
    }

    public void puntos1y1p(int npie){
        Point temp_uno=new Point(), temp_unop=new Point();
        Mat pieMat=new Mat();
        Mat piebinMat= new Mat();
        int b_fin=0, cant_blan=0, f_blan_mayor=0;
        if(npie==0){
            Utils.bitmapToMat(piederBitmap,pieMat);
        }else{
            Utils.bitmapToMat(pieizqBitmap,pieMat);
        }

        Imgproc.threshold(pieMat,piebinMat,240,255,Imgproc.THRESH_BINARY);
        int filas=piebinMat.height();
        int columnas=piebinMat.width();

        //Proceso para el punto "1"
        for (int i=0;i<Math.round(filas/2);i++){
            cant_blan=0;
            for (int j=0;j<columnas;j++){
                double[] valor_pixel=piebinMat.get(i, j);
                if(valor_pixel[0]==255){
                  cant_blan=cant_blan+1;
                }
            }
            if(cant_blan>f_blan_mayor){
                f_blan_mayor=cant_blan;
                temp_uno.y=i;
            }
        }

        int blan_tem=0, ini_i=(int) (temp_uno.y-f_blan_mayor*0.2), fin_i=(int) (temp_uno.y+f_blan_mayor*0.2);
        for (int j=0;j<columnas;j++){
            for (int i=ini_i;i<fin_i;i++){
                double[] valor_pixel=piebinMat.get(i, j);
                if(valor_pixel[0]==255){
                    blan_tem=0;
                    for(int k=j;k<columnas;k++){
                        double[] pixel2=piebinMat.get(i, k);
                        if (pixel2[0]==255){
                            blan_tem=blan_tem+1;
                        }else{
                            break;
                        }
                    }
                    if (blan_tem>=(cant_blan-cant_blan*0.1)){
                        temp_uno.y=i;
                        temp_uno.x=j;
                        b_fin=1;
                        break;
                    }
                }
            }
            if (b_fin==1){
                break;
            }
        }
        //Proceso para el punto "1'"
        b_fin=0;
        for (int j=0;j<columnas;j++){
            for (int i=Math.round(filas/2);i<filas;i++){
                double[] valor_pixel=piebinMat.get(i, j);
                if(valor_pixel[0]==255){
                    temp_unop.x=j;
                    temp_unop.y=i;
                    b_fin=1;
                    break;
                }
            }
            if (b_fin==1){
                break;
            }
        }
        //Proceso de rotar la imagen sobre el punto "1'"
        Mat dst_temp=new Mat();
        double m=0, angulo_radianes=0, angulo_grados=0;
        m=(temp_unop.y-temp_uno.y)/(temp_unop.x-temp_uno.x);
        angulo_radianes=Math.atan(m);
        angulo_grados=Math.toDegrees(angulo_radianes);
        if (angulo_grados>0){
            angulo_grados=angulo_grados-90;
        }else {
            angulo_grados=angulo_grados+90;
        }
        Mat r=Imgproc.getRotationMatrix2D(temp_unop,angulo_grados,1.0);
        Imgproc.warpAffine(piebinMat,dst_temp,r,piebinMat.size());
        if (npie==0){
            piederBitmap=Bitmap.createBitmap(dst_temp.width(), dst_temp.height(), Bitmap.Config.RGB_565);
            Utils.matToBitmap(dst_temp,piederBitmap);
        }else {
            pieizqBitmap=Bitmap.createBitmap(dst_temp.width(), dst_temp.height(), Bitmap.Config.RGB_565);
            Utils.matToBitmap(dst_temp,pieizqBitmap);
        }
        //Se busca el punto "1"
            for (int i=Math.round(dst_temp.height()/2);i>0;i--){
                double[] valor_pixel=dst_temp.get(i, (int) temp_unop.x);
                if(valor_pixel[0]==255){
                    temp_uno.x=temp_unop.x+1;
                    temp_uno.y=i;
                    break;
                }
            }
        //Se Guardan los puntos en las variables globales
        if (npie==0){
            unoder=temp_uno;
            unopder=temp_unop;
        }else {
            unoizq=temp_uno;
            unopizq=temp_unop;
        }
    }

    public void puntos2y2p (int npie){
        Mat pieMat=new Mat();
        Mat piebinMat= new Mat();
        Point temp_dos=new Point(), temp_dosp=new Point();
        int b_fin=0;
        if (npie==0){
            Utils.bitmapToMat(piederBitmap,pieMat);
        }else {
            Utils.bitmapToMat(pieizqBitmap,pieMat);
        }
        Imgproc.threshold(pieMat,piebinMat,240,255,Imgproc.THRESH_BINARY);
        int filas=piebinMat.height();
        int columnas=piebinMat.width();
        //Punto dos
        for (int i=0;i<filas;i++){
            for (int j=0;j<columnas;j++){
                double[] valor_pixel=piebinMat.get(i, j);
                if(valor_pixel[0]==255){
                    temp_dos.x=j;
                    temp_dos.y=i;
                    b_fin=1;
                    break;
                }
            }
            if(b_fin==1){
                break;
            }
        }
        //Punto dos primo
        b_fin=0;
        for (int i=filas-1;i>0;i--){
            for (int j=0;j<columnas;j++){
                double[] valor_pixel=piebinMat.get(i, j);
                if(valor_pixel[0]==255){
                    temp_dosp.x=j;
                    temp_dosp.y=i;
                    b_fin=1;
                    break;
                }
            }
            if(b_fin==1){
                break;
            }
        }
        //Se Guardan los puntos en las variables globales
        if (npie==0){
            dosder=temp_dos;
            dospder=temp_dosp;
        }else {
            dosizq=temp_dos;
            dospizq=temp_dosp;
        }
    }

    public void medfund(int npie){
        int DisTot=0;
        if(npie==0){
            Mfder=(int) (unoder.y-dosder.y);
            DisTot=(int) (dospder.y-dosder.y);
            Vmfder=DisTot/Mfder;
            if (Math.round(Vmfder)>Vmfder){
                Vmfder=Math.round(Vmfder)-1;
            }else{
                Vmfder=Math.round(Vmfder);
            }
        }else {
            Mfizq=(int) (unoizq.y-dosizq.y);
            DisTot=(int) (dospizq.y-dosizq.y);
            Vmfizq=DisTot/Mfizq;
            if (Math.round(Vmfizq)>Vmfizq){
                Vmfizq=Math.round(Vmfizq)-1;
            }else{
                Vmfizq=Math.round(Vmfizq);
            }
        }
    }

    public void valorx(int npie){
        Point temp_x=new Point();
        Point temp_dos;
        Mat pieMat=new Mat();
        Mat piebinMat= new Mat();
        int b_fin=0, temp_Mf;
        if(npie==0){
            temp_Mf=Mfder;
            temp_dos=dosder;
            Utils.bitmapToMat(piederBitmap,pieMat);
        }else {
            temp_Mf=Mfizq;
            temp_dos=dosizq;
            Utils.bitmapToMat(pieizqBitmap,pieMat);
        }
        Imgproc.threshold(pieMat,piebinMat,240,255,Imgproc.THRESH_BINARY);
        int columnas=piebinMat.width();
        for (int j=columnas-1;j>0;j--){
            for (int i=temp_Mf+(int)temp_dos.y;i<temp_Mf*2+(int)temp_dos.y;i++){
                double[] valor_pixel=piebinMat.get(i, j);
                if(valor_pixel[0]==255){
                    temp_x.x=j;
                    temp_x.y=i;
                    b_fin=1;
                    break;
                }
            }
            if (b_fin==1){
                break;
            }
        }
        //Se Guardan los puntos en las variables globales
        if (npie==0){
            x_der=temp_x;
        }else {
            x_izq=temp_x;
        }
    }

    public void valory(int npie){
        Point temp_p1y=new Point(),temp_p2y=new Point();
        Point temp_dos;
        Mat pieMat=new Mat();
        Mat piebinMat= new Mat();
        int temp_Mf;
        if (npie==0){
            temp_Mf=Mfder;
            temp_dos=dosder;
            Utils.bitmapToMat(piederBitmap,pieMat);
        }else {
            temp_Mf=Mfizq;
            temp_dos=dosizq;
            Utils.bitmapToMat(pieizqBitmap,pieMat);
        }
        Imgproc.threshold(pieMat,piebinMat,240,255,Imgproc.THRESH_BINARY);
        int columnas=piebinMat.width();
        for (int j=columnas-1;j>0;j--){
                double[] valor_pixel=piebinMat.get(temp_Mf*2+(int)temp_dos.y, j);
                if(valor_pixel[0]==255){
                    temp_p2y.x=j;
                    temp_p2y.y=temp_Mf*2+(int)temp_dos.y;
                    break;
                }
        }
        for (int j=0;j<columnas;j++){
            double[] valor_pixel=piebinMat.get(temp_Mf*2+(int)temp_dos.y, j);
            if(valor_pixel[0]==255){
                temp_p1y.x=j;
                temp_p1y.y=temp_Mf*2+(int)temp_dos.y;
                break;
            }
        }
        //Se Guardan los puntos en las variables globales
        if (npie==0){
            p1y_der=temp_p1y;
            p2y_der=temp_p2y;
        }else {
            p1y_izq=temp_p1y;
            p2y_izq=temp_p2y;
        }
    }

    public void dibujar_puntos(int npie){
        Point temp_uno, temp_unop, temp_dos, temp_dosp, temp_x, temp_p1y, temp_p2y;
        Mat pieMat=new Mat();
        if (npie==0){
            temp_uno=unoder;
            temp_unop=unopder;
            temp_dos=dosder;
            temp_dosp=dospder;
            temp_x=x_der;
            temp_p1y=p1y_der;
            temp_p2y=p2y_der;
            Utils.bitmapToMat(piederBitmap,pieMat);
        }else {
            temp_uno=unoizq;
            temp_unop=unopizq;
            temp_dos=dosizq;
            temp_dosp=dospizq;
            temp_x=x_izq;
            temp_p1y=p1y_izq;
            temp_p2y=p2y_izq;
            Utils.bitmapToMat(pieizqBitmap,pieMat);
        }
        //Se dibuja los puntos del pie derecho
        Imgproc.circle(pieMat,temp_uno,radPun,color_puntos,borPun);
        Imgproc.putText(pieMat,"1",temp_uno,1,3,color_texto,5);
        Imgproc.circle(pieMat,temp_unop,radPun,color_puntos,borPun);
        Imgproc.putText(pieMat,"1'",temp_unop,1,3,color_texto,5);
        Imgproc.circle(pieMat,temp_dosp,radPun,color_puntos,borPun);
        Imgproc.putText(pieMat,"2'",temp_dosp,1,3,color_texto,5);
        Imgproc.circle(pieMat,temp_dos,radPun,color_puntos,borPun);
        Imgproc.putText(pieMat,"2",temp_dos,1,3,color_texto,5);
        Point p_temp=new Point();
        p_temp.y=temp_x.y;
        p_temp.x=temp_unop.x;
        Imgproc.circle(pieMat,p_temp,radPun,color_puntos,borPun);
        Imgproc.circle(pieMat,temp_x,radPun,color_puntos,borPun);
        //Imgproc.circle(pieMat,x_der,radPun,color_puntos,borPun);
        Imgproc.circle(pieMat,temp_p1y,radPun,color_puntos,borPun);
        Imgproc.circle(pieMat,temp_p2y,radPun,color_puntos,borPun);
        if (npie==0){
            Utils.matToBitmap(pieMat,piederBitmap);
        }else {
            Utils.matToBitmap(pieMat,pieizqBitmap);
        }
    }

    public void dibujar_lineas (int npie){
        Point temp_uno, temp_unop, temp_dos, temp_dosp, temp_x, temp_p1y, temp_p2y;
        Point p1=new Point();
        Point p2=new Point();
        double m=0, b=0, temp_Vmf=0;
        int filas=0, columnas=0, temp_Mf=0;
        Mat pieMat=new Mat();
        if (npie==0){
            temp_uno=unoder;
            temp_unop=unopder;
            temp_dos=dosder;
            temp_dosp=dospder;
            temp_Mf=Mfder;
            temp_Vmf=Vmfder;
            temp_x=x_der;
            temp_p1y=p1y_der;
            temp_p2y=p2y_der;
            Utils.bitmapToMat(piederBitmap,pieMat);
        }else {
            temp_uno=unoizq;
            temp_unop=unopizq;
            temp_dos=dosizq;
            temp_dosp=dospizq;
            temp_Mf=Mfizq;
            temp_Vmf=Vmfizq;
            temp_x=x_izq;
            temp_p1y=p1y_izq;
            temp_p2y=p2y_izq;
            Utils.bitmapToMat(pieizqBitmap,pieMat);
        }
        filas=pieMat.height();
        columnas=pieMat.width();
        //Puntos de la linea principal
        m=(temp_unop.y-temp_uno.y)/(temp_unop.x-temp_uno.x);
        b=temp_uno.y-m*temp_uno.x;
        p1.y=1;
        p1.x=(p1.y-b)/m;
        p2.y=filas-1;
        p2.x=(p2.y-b)/m;
        Imgproc.line(pieMat,p1,p2,color_lineas,2);


        //Puntos de la linea Superior
        m=0;
        b=temp_dos.y-m*temp_dos.x;
        p1.x=1;
        p1.y=b+m*p1.x;
        p2.x=columnas-1;
        p2.y=b+m*p2.x;
        Imgproc.line(pieMat,p1,p2,color_lineas,2);

        //Puntos de la linea Inferior
        m=0;
        b=temp_dosp.y-m*temp_dosp.x;
        p1.x=1;
        p1.y=b+m*p1.x;
        p2.x=columnas-1;
        p2.y=b+m*p2.x;
        Imgproc.line(pieMat,p1,p2,color_lineas,2);

        //Dibujamos la lineas fundamentales
        for(int i=1;i<=temp_Vmf;i++){
            m=0;
            b=i*temp_Mf+temp_dos.y;
            p1.x=1;
            p1.y=b+m*p1.x;
            p2.x=columnas-1;
            p2.y=b+m*p2.x;
            Imgproc.line(pieMat,p1,p2,color_linfun,2);
        }

        //Dibujamos la linea x
        p1.x=temp_unop.x;
        p1.y=temp_x.y;
        Imgproc.line(pieMat,p1,temp_x,color_lineaxy,3);

        p1.x=Math.round((temp_x.x-temp_unop.x)/2);
        p1.y=temp_x.y;
        Imgproc.putText(pieMat,"X",temp_x,1,3,color_texto,5);

        p1.x=temp_x.x;
        p1.y=temp_Mf+temp_dos.y;
        p2.x=temp_x.x;
        p2.y=temp_Mf*2+temp_dos.y;
        Imgproc.line(pieMat,p1,p2,color_linsec,2);

        //Dibujamos las lineas de y

        Imgproc.putText(pieMat,"Y",temp_p1y,1,3,color_texto,5);
        Imgproc.line(pieMat,temp_p1y,temp_p2y,color_lineaxy,3);
        p1.x=temp_p1y.x;
        p1.y=temp_Mf*3+temp_dos.y;
        Imgproc.line(pieMat,p1,temp_p1y,color_linsec,2);
        p1.x=temp_p2y.x;
        p1.y=temp_Mf*3+temp_dos.y;
        Imgproc.line(pieMat,p1,temp_p2y,color_linsec,2);

        //Guardamos el mat en el bitmap para Visualizarlo
        if (npie==0){
            Utils.matToBitmap(pieMat,piederBitmap);
        }else {
            Utils.matToBitmap(pieMat,pieizqBitmap);
        }
    }

    public void espejo (int npie){
        Mat pieMat=new Mat(), pie_temp=new Mat();
        if(npie==0){
            Utils.bitmapToMat(piederBitmap,pieMat);
        }else {
            Utils.bitmapToMat(pieizqBitmap,pieMat);
        }
        pie_temp=pieMat.clone();
        int filas=pieMat.height();
        int columnas=pieMat.width();
        for (int i=0;i<filas;i++){
            for (int j=0;j<columnas;j++){
                double[] valor_pixel=pieMat.get(i,columnas-1-j);
                pie_temp.put(i, j, valor_pixel);
            }
        }
        if(npie==0){
            Utils.matToBitmap(pie_temp,piederBitmap);
        }else {
            Utils.matToBitmap(pie_temp,pieizqBitmap);
        }
    }

    public void ajuste_punto(int npie){
        Mat pieMat=new Mat();
        if(npie==0){
            Utils.bitmapToMat(piederBitmap,pieMat);
        }else {
            Utils.bitmapToMat(pieizqBitmap,pieMat);
            int columnas=pieMat.width();
            unoizq.x=columnas-1-unoizq.x;
            unopizq.x=columnas-1-unopizq.x;
            dosizq.x=columnas-1-dosizq.x;
            dospizq.x=columnas-1-dospizq.x;
            x_izq.x=columnas-1-x_izq.x;
            double temp_p1y=p1y_izq.x;
            p1y_izq.x=columnas-1-p2y_izq.x;
            p2y_izq.x=columnas-1-temp_p1y;
        }
    }

}
