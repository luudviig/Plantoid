package com.example.plantoid;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.text.Editable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.plantoid.models.Plant;
import com.example.plantoid.models.SocketInformation;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.NoRouteToHostException;
import java.net.Socket;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MAIN_ACTIVITY";

    private Handler handler;
    private MenuItem updateButton;
    private LinearLayout dataBody;
    private TextView plantText;
    private Thread pollingThread;
    private ArrayList<Plant> plants;
    private boolean dropDownArrowVisible;
    private boolean terminate;
    private SocketInformation socketInformation;
    private boolean updateBtnVisible;
    private String exceptionMessage;
    private boolean pollingError;
    private AnimationDrawable animation;

    //LIFECYCLES
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        updateBtnVisible = true;
        dropDownArrowVisible = true;
        exceptionMessage = "";
        pollingError = false;
        plants = new ArrayList<>();

        handler = new Handler();
        plantText = findViewById(R.id.plant_text);
        dataBody = findViewById(R.id.dataBody);
        updateButton = findViewById(R.id.updateButton);

        setOnTouchDropDown();
        Log.v(TAG, "IN ONCREATE");
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull String name, @NonNull Context context, @NonNull AttributeSet attrs) {
        Log.v(TAG, "IN ONCREATEVIEW");
        return super.onCreateView(name, context, attrs);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.v(TAG, "IN ONSTART");
    }

    @Override
    protected void onResume() {
        super.onResume();
        readUserDataFromCache();
        startPollingThread();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.v(TAG, "IN ONPAUSE");
        terminate = true;
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.v(TAG, "IN ONSTOP");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.v(TAG, "IN ONDESTROY");
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.custom_toolbar_menu, menu);
        updateButton = menu.findItem(R.id.updateButton);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.updateButton:
                requestDataFromHub();
                return true;
            case R.id.homeButton:
                if (!pollingThread.isAlive()) {
                    startPollingThread();
                }
                loadPlantsIntoUI();
                return true;
            case R.id.settingsButton:
                terminate = true;
                loadIpPortWindow();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (updateBtnVisible) {
            updateButton.setEnabled(true);
            updateButton.getIcon().setAlpha(255);
        } else {
            updateButton.setEnabled(false);
            updateButton.getIcon().setAlpha(50);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    //CUSTOM METHODS
    private void startPollingThread() {
        terminate = false;
        pollingThread = new Thread(new Runnable() {
            @Override
            public void run() {
                pollHub();
            }
        });

        pollingThread.start();
    }

    private void pollHub() {
        while (!terminate) {
            Log.v(TAG, "POLLING\n");
            requestDataFromHub();
            SystemClock.sleep(10 * 1000);
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setOnTouchDropDown() {
        TextView textView = findViewById(R.id.plant_text);

        textView.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                Drawable[] drawables = textView.getCompoundDrawables();
                Drawable rightDrawable = drawables[2];
                Drawable dropArrow = getResources().getDrawable(R.drawable.drop_down_arrow);
                Drawable upArrow = getResources().getDrawable(R.drawable.drop_up_arrow);
                final int DRAWABLE_RIGHT = 2;

                if ((event.getRawX() >= (textView.getRight() - textView.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width()))) {
                    if (rightDrawable.getConstantState().equals(dropArrow.getConstantState())) {
                        dataBody.removeAllViews();
                        textView.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.drop_up_arrow, 0);
                        dropDownArrowVisible = false;
                    } else if (rightDrawable.getConstantState().equals(upArrow.getConstantState())) {
                        loadPlantsIntoUI();
                        textView.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.drop_down_arrow, 0);
                        dropDownArrowVisible = true;
                    }
                    return true;
                }
            }
            return false;
        });
    }

    private void requestDataFromHub() {
        pollingError = false;
        updateBtnVisible = false;
        invalidateOptionsMenu();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Socket socket = new Socket();
                    socket.connect(new InetSocketAddress(socketInformation.getIp(), socketInformation.getPort()), 4500);
                    socket.setSoTimeout(2500);
                    BufferedWriter output = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                    BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                    output.write("1000".concat("\n"));
                    output.flush();

                    String messageFromServer = input.readLine();

                    String[] commands = messageFromServer.split("::");

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (commands[0].equals("1001")) {
                                if (commands[1].equals("0")) {
                                    dataBody.removeAllViews();
                                    plants.clear();
                                    makeToast("No Active Plants");
                                } else {
                                    loadPlantsIntoPlantlist(commands);
                                }
                            } else {
                                dataBody.removeAllViews();
                                plants.clear();
                            }
                        }
                    });

                } catch (ConnectException e) {
                    e.printStackTrace();
                    pollingError = true;
                    exceptionMessage = "Problem connecting with hub";
                } catch (NoRouteToHostException e) {
                    e.printStackTrace();
                    pollingError = true;
                    exceptionMessage = "Problem connecting with hub";
                } catch (IOException e) {
                    e.printStackTrace();
                    pollingError = true;
                    exceptionMessage = "Problem settings up stream with hub";
                } finally {
                    if (pollingError) {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                plants.clear();
                                dataBody.removeAllViews();
                                makeToast(exceptionMessage);
                            }
                        });
                    }
                }
            }
        }).start();
    }

    private void loadPlantsIntoPlantlist(String[] commands) {
        plants.clear();
        int amountOfPlants = Integer.parseInt(commands[1]);

        setHubName(commands[10]);

        int count = 2;
        for (int i = 0; i < amountOfPlants; i++) {
            int id = Integer.parseInt(commands[count++]);
            String alias = String.valueOf(commands[count++]);
            String lastWatered = String.valueOf(commands[count++]);
            String lastPolled = String.valueOf(commands[count++]);
            double soilHumidity = Double.parseDouble(commands[count++]);
            double airHumidity = Double.parseDouble(commands[count++]);
            double airTemp = Double.parseDouble(commands[count++]);
            double lightExposure = Double.parseDouble(commands[count++]);
            boolean waterTankEmpty = Boolean.parseBoolean(commands[count + 1]);
            Plant plant = new Plant(id, alias, lastWatered, lastPolled, soilHumidity, airHumidity, airTemp, lightExposure, waterTankEmpty);
            plants.add(plant);
        }
        if (dropDownArrowVisible) {
            loadPlantsIntoUI();
        } else {
            SystemClock.sleep(50);
            updateBtnVisible = true;
            invalidateOptionsMenu();
        }
    }

    private void loadIpPortWindow() {
        updateBtnVisible = false;
        invalidateOptionsMenu();
        plantText.setVisibility(View.INVISIBLE);
        dataBody.removeAllViews();

        View ipPortWindow = getLayoutInflater().inflate(R.layout.ip_port_window, null);

        EditText portField = ipPortWindow.findViewById(R.id.portTextField);
        EditText ipField = ipPortWindow.findViewById(R.id.ipTextField);
        Button settingsOkButton = ipPortWindow.findViewById(R.id.settingsOkButton);
        Button makeDefaultButton = ipPortWindow.findViewById(R.id.makeDefaultButton);

        settingsOkButton.setOnClickListener(v -> {
            socketInformation.setPort(Integer.parseInt(portField.getText().toString()));
            socketInformation.setIp(ipField.getText().toString());
            writeSockeInfoToCache();
            Log.v(TAG, "PORT: " + socketInformation.getPort());
            Log.v(TAG, "IP: " + socketInformation.getIp());
        });

        makeDefaultButton.setOnClickListener(v -> {
            portField.setText(R.string.original_port);
            ipField.setText(R.string.original_ip);
        });

        portField.setText(String.valueOf(socketInformation.getPort()));
        ipField.setText(String.valueOf(socketInformation.getIp()));

        //add view to dataBody
        dataBody.addView(ipPortWindow);
    }

    private void loadPlantsIntoUI() {
        plantText.setVisibility(View.VISIBLE);
        dataBody.removeAllViews();
        for (int i = 0; i < plants.size(); i++) {
            Plant plant = plants.get(i);
            View plantBox = getLayoutInflater().inflate(R.layout.plant_box, null);

            TextView alias = plantBox.findViewById(R.id.plant_alias);
            TextView id = plantBox.findViewById(R.id.plant_id);
            TextView lastWatered = plantBox.findViewById(R.id.plant_lastWatered);
            TextView lastPolled = plantBox.findViewById(R.id.plant_lastPolled);
            TextView soilHumidity = plantBox.findViewById(R.id.plant_soilhumidity);
            TextView airHumidity = plantBox.findViewById(R.id.plant_humidity);
            TextView airTemp = plantBox.findViewById(R.id.plant_airTemp);
            TextView lightExposure = plantBox.findViewById(R.id.plant_lightExposure);
            TextView waterTankEmpty = plantBox.findViewById(R.id.waterTankEmpty);

            alias.setText(plant.getAlias());
            id.setText(String.valueOf(plant.getId()));
            lastWatered.setText(plant.getLastWatered());
            lastPolled.setText(plant.getLastPolled());
            soilHumidity.setText(String.valueOf(plant.getSoilHumidity()));
            airHumidity.setText((plant.getAirHumidity()) + " \u0025"); //Percent symbol
            airTemp.setText((plant.getAirTemp()) + " \u2103"); //Celsius symbol
            lightExposure.setText(String.valueOf(plant.getLightExposure()));
            waterTankEmpty.setText(String.valueOf(plant.isWaterTankEmpty()));

            dataBody.addView(plantBox);
        }

        //We sleep the thread for a short time the make the userExperience better.
        SystemClock.sleep(50);
        updateBtnVisible = true;
        invalidateOptionsMenu();
    }

    private void setHubName(String hubName) {
        ActionBar actionBar = getSupportActionBar();
        if (!actionBar.equals(null)) {
            if (hubName.equals("") || hubName.equals(null)) {
                actionBar.setTitle("Plantoid");
            } else {
                actionBar.setTitle(hubName);
            }
        }
    }

    public void writeSockeInfoToCache() {
        String filePath = getCacheDir() + "socketdata";

        try (ObjectOutputStream objectOutput = new ObjectOutputStream(new FileOutputStream(new File(filePath)))) {
            //correct path?

            objectOutput.writeObject(socketInformation);

        } catch (Exception e) {
            e.printStackTrace();
            makeToast("Unable to write user to cache");
        }
    }

    private void readUserDataFromCache() {
        try (ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream(new File(getCacheDir() + "socketdata")))) {
            //Reads from "file" and casts to LoggedInUser-object.
            socketInformation = (SocketInformation) objectInputStream.readObject();

            if (socketInformation.equals(null) || socketInformation.getIp().equals("") || socketInformation.getPort() == 0) {
                socketInformation = new SocketInformation(String.valueOf(R.string.original_ip), R.string.original_port);
            }

        } catch (Exception e) {
            socketInformation = new SocketInformation("192.168.1.102", 7777);
            makeToast("Problem reading cache");
        }
    }

    private void makeToast(String toastMessage) {
        Toast toast = Toast.makeText(getApplicationContext(), toastMessage, Toast.LENGTH_SHORT);
        toast.show();
    }

    /*private void setupAnimation() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.hide();
        plantText.setVisibility(View.INVISIBLE);
        dataBody.removeAllViews();


        ImageView i = new ImageView(this);
        i.setBackgroundResource(R.drawable.animation);
        animation = (AnimationDrawable) i.getBackground();
        dataBody.addView(i);
        animation.start();
    }*/


    private void printPlants() {
        Log.v(TAG, "=====================");
        for (Plant p : plants) {
            Log.v(TAG, "Polled: " + p.getLastPolled() + "\n" +
                    "Watered: " + p.getLastWatered() + "\n" +
                    "Airhumidity: " + p.getAirHumidity() + "\n" +
                    "Airtemp: " + p.getAirTemp() + "\n" +
                    "Soilhumidity: " + p.getSoilHumidity() + "\n" +
                    "Lightexposure: " + p.getLightExposure() + "\n");
        }
        Log.v(TAG, "=====================");
    }
}