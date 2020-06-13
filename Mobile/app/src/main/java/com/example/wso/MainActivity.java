package com.example.wso;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;

public class MainActivity extends AppCompatActivity {
    //https://git.kernel.org/pub/scm/linux/kernel/git/torvalds/linux.git/tree/include/uapi/linux/input-event-codes.h
    private final String[] SIGNS = {"RESERVED_KEY", "ECS", "1", "2", "3", "4", "5", "6", "7", "8", "9", "0", "-", "=", "BACKSPACE"
            , "TAB", "q","w","e","r", "t", "y", "u", "i", "o", "p", "[", "]", "ENTER", "L_CTRL", "a" ,"s" ,"d", "f", "g", "h", "j", "k", "l",
            ";", "'", "`", "L_SHIFT", "\\", "z", "x", "c", "v", "b", "n", "m", ",", ".", "/", "R_SHIFT", "*", "L_ALT", " ", "CAPSLOCK",
            "F1", "F2", "F3", "F4", "F5", "F6", "F7", "F8", "F9", "F10", "NUMLOCK", "SCROLLOCK", "7", "8", "9", "-", "4", "5", "6", "+", "1",
            "2", "3", "0", ".", "NOVAL", "NOVAL", "INTERNATIONAL", "F11", "F12", "NOVAL", "NOVAL", "NOVAL", "NOVAL", "NOVAL", "NOVAL",
            "NOVAL", "RETURN", "CTRLRIGHT", "/", "PRINTSCRN", "ALTRIGHT", "NOVAL", "HOME", "UP", "PAGEUP", "LEFT", "RIGHT", "END", "DOWN",
            "PAGEDOWN", "INSERT", "DELETE", "NOVAL", "NOVAL", "NOVAL", "NOVAL", "NOVAL", "NOVAL", "NOVAL", "PAUSE",
            "NOVAL", "NOVAL", "NOVAL", "NOVAL", "NOVAL", "LOGOLEFT", "LOGORIGHT", "NOVAL", "NOVAL", "NOVAL"};

    private TextView text;
    private EditText ipText;
    private EditText loginText;
    private EditText passwordText;
    private Button connectButton;
    private Button disconnectButton;
    private boolean isLoop;
    private boolean isCapsLock;

    private JSch jsch;
    private Session session;
    private Channel channel;
    private ChannelSftp sftpChannel;
    private InputStream stream;

    private AsyncTask<Void, Void, Void> taskA;
    private AsyncTask<Void, Void, Void> taskB;
    private AsyncTask<Void, Void, Void> taskC;
    private int now = 0;
    private int prev = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        text = (TextView)findViewById(R.id.text);

        ipText = (EditText)findViewById(R.id.ip);
        loginText = (EditText)findViewById(R.id.login);
        passwordText = (EditText)findViewById(R.id.password);

        connectButton = (Button)findViewById(R.id.connectButton);
        disconnectButton = (Button)findViewById(R.id.disconnectButton);

        stream = null;

        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String ip = ipText.getText().toString();
                String login = loginText.getText().toString();
                String password = passwordText.getText().toString();

                try {
                    ssh(ip, login, password);
                    loop(ip, login, password);
                } catch (JSchException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void parseText(String line) {
        line = SIGNS[Integer.parseInt(line)];

        if(line == "ENTER")
            text.setText(text.getText() + "\n");
        else if(line == "TAB")
            text.setText(text.getText() + "\t");
        else if(line == "BACKSPACE")
            text.setText(text.getText().toString().substring(0, text.getText().length() - 1));
        else if(line == "CAPSLOCK")
            isCapsLock = !isCapsLock;
        else if(line == "CAPSLOCK")
            isCapsLock = !isCapsLock;
        else {
            if(isCapsLock)
                text.setText(text.getText() + line.toUpperCase());
            else
                text.setText(text.getText() + line);
        }
    }

    @SuppressLint("StaticFieldLeak")
    public void loop(final String ip, final String login, final String password) throws InterruptedException {
        isLoop = true;

        taskA = new AsyncTask<Void, Void, Void>() {
            @SuppressLint("WrongThread")
            @Override
            protected Void doInBackground(Void... voids) {
                while(isLoop) {
                    try {
                        stream = sftpChannel.get("/home/pi/Desktop/output_file.txt");

                    } catch (NullPointerException | SftpException e) {}

                    try {
                        BufferedReader br = new BufferedReader(new InputStreamReader(stream));

                        String line, last = "";
                        now = 0;
                        while ((line = br.readLine()) != null) {
                            last = line;
                            now++;
                        }
                        if(now > prev && Integer.parseInt(last) < SIGNS.length) {
                            parseText(last);
                        }
                        prev = now;

                    } catch (IOException io) {
                        System.out.println("Exception: " + io.getMessage());
                        io.getMessage();
                    } catch (Exception e) {
                        System.out.println("Exception: " + e.getMessage());
                        e.getMessage();
                    }
                }
                return null;
            }
        };

        taskB = new AsyncTask<Void, Void, Void>() {
            @SuppressLint("WrongThread")
            @Override
            protected Void doInBackground(Void... voids) {
                disconnectButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        isLoop = false;
                        taskA.cancel(true);
                        sftpChannel.exit();
                        channel.disconnect();
                        session.disconnect();
                        taskC.cancel(true);
                        text.setText("");
                        Log.i("Disconnect","Zakonczono polaczenie");
                    }
                });
                return null;
            }
        };

        taskC.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        taskA.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        taskB.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @SuppressLint("StaticFieldLeak")
    public void ssh(final String ip, final String login, final String password) throws JSchException {
        taskC = new AsyncTask<Void, Void, Void>() {
            @SuppressLint("WrongThread")
            @Override
            protected Void doInBackground(Void... voids) {
                jsch = new JSch();
                try {
                    session = jsch.getSession(login, ip, 22);
                    session.setConfig("StrictHostKeyChecking", "no");
                    session.setPassword(password);
                    session.connect();

                    channel = session.openChannel("sftp");
                    channel.connect();
                    sftpChannel = (ChannelSftp) channel;

                    text.setText("");
                    Log.i("Connect","Polaczono");
                } catch (JSchException e) {
                    e.printStackTrace();
                }
                return null;
            }
        };
    }
}

