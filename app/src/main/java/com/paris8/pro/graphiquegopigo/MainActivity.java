package com.paris8.pro.graphiquegopigo;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Display;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.Viewport;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Random;

/**
 * CLASSE PRINCIPALE CONTENANT L'ACTIVITE PRINCIPALE
 * PRINCIPAL CLASS CONTAINING THE MAIN ACTIVITY
 */
public class MainActivity extends AppCompatActivity {

    //Variables recuperees du gopigo
    //Variables recovered from gopigo
    String distance="0";
    String temps;
    String etat_gps;
    String n_s;
    String e_o;
    String latitude;
    String longitude;

    //Variables pour utiliser la classe MySurfaceView
    //Variables to use the MySurfaceView class
    MySurfaceView mySurfaceView;

    //Variables pour la connexion à la BDD
    // Variables for connecting to the BDD
    String result=null;
    HttpURLConnection connection;

    //Variables pour le dessin du graphique
    //Variables for drawing the graph
    int boucle=0;
    boolean pause=false;
    private LineGraphSeries<DataPoint> seriesA;
    private static final Random RANDOM = new Random();
    GraphView graph;
    Thread nouveauThreadA;
    Button Bdessine, Bpause, Btrajectoire;
    double a, b;

    /**
     * Creation de l'activite principale
     * Listeners pour les boutons de l'interface principale
     * pour lancer les fonctions de dessin du graphique mais aussi pour afficher la SurfaceView
     * Creation of the main activity
     * Listeners for buttons on the main interface
     * To start the drawing functions of the graphic but also to
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mySurfaceView = new MySurfaceView(this);

        setContentView(R.layout.activity_main);
        new MyTask().execute();

        graph = (GraphView) findViewById(R.id.graph);
        Bdessine = (Button) findViewById(R.id.bouton_dessine);
        Bpause = (Button) findViewById(R.id.bouton_pause);
        Btrajectoire  = (Button) findViewById(R.id.trajectoire);

        construireGraph();

        Bdessine.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pause=false;
                dessinerGraph();
            }
        });

        Btrajectoire.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setContentView(mySurfaceView);
            }
        });

        Bpause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pause=true;
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        mySurfaceView.onPauseMySurfaceView();
    }

    /**
     * Demarrer le thread principal qui va demarrer le dessin du graphique
     * Start the main thread that will start the drawing of the graph
     */
    private void dessinerGraph() {
        if (!nouveauThreadA.isAlive() && seriesA.isEmpty()) {
            nouveauThreadA.start();
        }
    }

    /**
     * Genere et assemble les donnees de tous les points récupérées depuis la BDD pour en faire une courbe
     * Genere and assembles the data of all the points recovered from the BDD to make a curve
     * @param i
     */
    private void genDonnees(int i) {
        b = Integer.parseInt(distance); //val distance
        seriesA.appendData(new DataPoint(a++, b), false, i);
    }

    /**
     * Methode permettant de construire et de parametrer la GraphView ainsi que les courbes
     * Method for constructing and parameterizing the GraphView as well as the curves
     */
    private void construireGraph() {
        a = 0;

        graph.setTitle("Distance");
        graph.setTitleTextSize(40);
        graph.setTitleColor(Color.BLUE);
        graph.getGridLabelRenderer().setHorizontalAxisTitle("temps");
        graph.getGridLabelRenderer().setVerticalAxisTitle("distance");
        graph.getGridLabelRenderer().setHorizontalLabelsVisible(false);
        Viewport viewport = graph.getViewport();
        viewport.setYAxisBoundsManual(true);
        viewport.setMinY(0);
        viewport.setMaxY(100);
        viewport.setXAxisBoundsManual(true);
        viewport.setMinX(0);
        viewport.setMaxX(50);
        viewport.setScalable(true);

        seriesA = new LineGraphSeries<>();
        seriesA.setColor(Color.RED);

        graph.addSeries(seriesA);
    }

    /**
     * Methode permettant de lancer dans un thread le dessin de la courbe
     * on peut arreter et reprendre le dessin avec les bouton pause et dessiner qui change la variable boolean
     * Method to launch in a thread the drawing of the curve
     * You can stop and resume the drawing with the pause button and draw that changes the boolean variable
     */
    @Override
    protected void onResume() {
        super.onResume();
        mySurfaceView.onResumeMySurfaceView();
        nouveauThreadA = new Thread(new Runnable() {
            @Override
            public void run() {
                boucle=0;
                while (true) {
                    if(pause==false) {

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                genDonnees(boucle);
                            }
                        });
                        boucle++;
                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        });
    }


    /**
     * CLASSE ASYNCTASK
     * Connexion a la BDD
     * Recuperation des diverses informations fournies par le module GPS
     * CLASS ASYNCTASK
     * Connect to the BDD
     * Recovery of various information provided by the GPS module
     */
    private class MyTask extends AsyncTask<Void,Void,Void>{

        @Override
        protected Void doInBackground(Void... params) {
            try
            {
                while(true) {
                    URL url = new URL("http://facecuttest2.kalanda.info/Facecut_WEB/select.php");
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestProperty("User-Agent", "");
                    connection.setRequestMethod("POST");
                    connection.setDoInput(true);
                    connection.connect();
                    InputStream inputStream = connection.getInputStream();
                    BufferedReader rd = new BufferedReader(new InputStreamReader(inputStream));

                    Log.e("etape 1", "connexion reussie ");

                    StringBuilder sb = new StringBuilder();
                    String line = "";
                    while ((line = rd.readLine()) != null) {
                        if(line != null && !line.isEmpty() && !line.equals("null")) {
                            sb.append(line + "\n");
                        }
                    }
                    result = sb.toString();

                    if(result!="") {
                        JSONObject json_data = new JSONObject(result);
                        distance = (json_data.getString("distance"));
                        Log.e("Distance: ", distance);
                        temps = (json_data.getString("temps"));
                        Log.e("temps: ", temps);
                        etat_gps = (json_data.getString("etat_gps"));
                        Log.e("etat_gps: ", etat_gps);
                        n_s = (json_data.getString("n_s"));
                        Log.e("n_s: ", n_s);
                        e_o = (json_data.getString("e_o"));
                        Log.e("e_o: ", e_o);
                        latitude = (json_data.getString("latitude"));
                        Log.e("latitude: ", latitude);
                        longitude = (json_data.getString("longitude"));
                        Log.e("longitude: ", longitude);
                    }
                }
            }
            catch(Exception e) {
                Log.e("Erreur", e.toString());
            }
            return null;
        }
    }


    /**
     * CLASSE SURFACEVIEW
     * Permet le dessin de la trajectoire du robot
     * CLASS SURFACEVIEW
     * Allows the drawing of the trajectory of the robot
     */
    class MySurfaceView extends SurfaceView implements Runnable {

        Path path;

        Thread thread = null;
        SurfaceHolder surfaceHolder;
        Canvas canvas;

        volatile boolean running = false;

        private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        Random r, aa, bb;

        float a;
        float b;

        public MySurfaceView(Context context) {
            super(context);
            surfaceHolder = getHolder();
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(15);
            paint.setColor(Color.WHITE);
            r = new Random();
            aa = new Random();
            bb = new Random();
            Display display = getWindowManager().getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            a = (size.x)/2;
            b = (size.y)/2;

        }

        public void onResumeMySurfaceView() {
            running = true;
            thread = new Thread(this);
            thread.start();
        }

        public void onPauseMySurfaceView() {
            boolean retry = true;
            running = false;
            while (retry) {
                try {
                    thread.join();
                    retry = false;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        /**
         * Methode recursive pour dessiner la trajectoire du robot sur le SurfaceView
         * Recursive method to draw the trajectory of the robot on the SurfaceView
         */
        private void bougerRobot() {
            int commande = traduireDirection();

            int var = commande;
            surfaceHolder.getSurface(); // Enlever pour voir déplacement sans mémorisation
            canvas = surfaceHolder.lockCanvas();

            switch (var) {
                case 1: //nord est      NE
                    a += 10;
                    b -= 10;
                    //path.rLineTo(a, b);
                    canvas.drawCircle(a, b, 4, paint);
                    surfaceHolder.unlockCanvasAndPost(canvas);
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }                    bougerRobot();
                    break;
                case 2: //nord ouest    NO
                    a -= 10;
                    b -= 10;
                    canvas.drawCircle(a, b, 4, paint);
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    bougerRobot();
                    break;
                case 3: //sud est       SE
                    a += 10;
                    b += 10;
                    canvas.drawCircle(a, b, 4, paint);
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    bougerRobot();
                    break;
                case 4: //sud ouest     SO
                    a -= 10;
                    b += 10;
                    canvas.drawCircle(a, b, 4, paint);
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    bougerRobot();
                    break;
                case 0: //stop
                    path.close();
                    bougerRobot();
                    break;
                default:
                    break;
            }
        }

        /**
         * Methode qui recupere la direction renvoyée par le module GPS
         * et qui traduit cette direction en commande pour pouvoir dessiner la trajectoire
         * Method that retrieves the direction returned by the GPS module
         * And which translates this direction in order to be able to draw the trajectory
         * @return
         */
        private int traduireDirection(){
            int commande;
            String direction1 = n_s;
            String direction2 = e_o;

            if (direction1.equals("N") && direction2.equals("E")) {
                commande = 1;
                return commande;
            }
            else if(direction1.equals("N") && direction2.equals("O")){
                commande = 2;
                return commande;
            }
            else if(direction1.equals("S") && direction2.equals("E")){
                commande = 3;
                return commande;
            }
            else if(direction1.equals("S") && direction2.equals("O")){
                commande = 4;
                return commande;
            }
            else if(direction1.equals("") && direction2.equals("")){
                commande = 0;
                return commande;
            }
            else if(direction1.equals("") || direction2.equals("")){
                commande = 0;
                return commande;
            }

            return 0;
        }

        /**
         * Methode run qui execute le dessin de la trajectoire dans un thread
         * Methode run that executes the drawing of the trajectory in a thread
         */
        @Override
        public void run() {
            // TODO Auto-generated method stub
            while (running) {
                if (surfaceHolder.getSurface().isValid()) {

                    path = new Path();
                    path.moveTo(1280, 720);
                    bougerRobot();

                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

}

