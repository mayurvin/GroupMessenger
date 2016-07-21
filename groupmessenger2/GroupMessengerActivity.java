package edu.buffalo.cse.cse486586.groupmessenger2;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 * 
 * @author stevko
 *
 */
public class GroupMessengerActivity extends Activity {
    static final String TAG = GroupMessengerActivity.class.getSimpleName();
    static final String REMOTE_PORT0 = "11108";
    static final String REMOTE_PORT1 = "11112";
    static final String REMOTE_PORT2 = "11116";
    static final String REMOTE_PORT3 = "11120";
    static final String REMOTE_PORT4 = "11124";
    static final int SERVER_PORT = 10000;
    static final String[] allRemotePorts = {REMOTE_PORT0, REMOTE_PORT1, REMOTE_PORT2, REMOTE_PORT3, REMOTE_PORT4};
    static private int keyCount = 0;
    private ArrayList<MessageClass> msgObjectList = new ArrayList<MessageClass>();
    private int proposed = 1;
    private int aliveNodes = 5;
    private int procNum = 0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);

        /*
         * TODO: Use the TextView to display your messages. Though there is no grading component
         * on how you display the messages, if you implement it, it'll make your debugging easier.
         */
        TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());
        
        /*
         * Registers OnPTestClickListener for "button1" in the layout, which is the "PTest" button.
         * OnPTestClickListener demonstrates how to access a ContentProvider.
         */
        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));
        
        /*
         * TODO: You need to register and implement an OnClickListener for the "Send" button.
         * In your implementation you need to get the message from the input box (EditText)
         * and send it to other AVDs.
         */
        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        final String myPort = String.valueOf((Integer.parseInt(portStr) * 2));

        try {

            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (IOException e) {
            Log.e(TAG, "Can't create a ServerSocket");
            return;
        }

        final Button sendButton = (Button) findViewById(R.id.button4);
        final TextView editText = (TextView) findViewById(R.id.editText1);

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String msg = editText.getText().toString() + "\n";
                editText.setText(""); // This is one way to reset the input box.
                TextView localTextView = (TextView) findViewById(R.id.textView1);
                localTextView.append("\t" + msg); // This is one way to display a string.

                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, myPort);
                return ;
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }

    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {

        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];

            /*
             * TODO: Fill in your server code that receives messages and passes them
             * to onProgressUpdate().
             */
            Socket inSocket = null;
            do {
                try{
                    inSocket = serverSocket.accept();
                    DataInputStream in = new DataInputStream(inSocket.getInputStream());
                    String msgRecieved = in.readUTF();
                    String[] msgRecievedArray = msgRecieved.split("::");
                    MessageClass messageRcvd = new MessageClass();
                    messageRcvd.msgType = msgRecievedArray[0];
                    messageRcvd.msgPriority = Integer.parseInt(msgRecievedArray[1]);
                    messageRcvd.msgProcNum = Integer.parseInt(msgRecievedArray[2]);
                    messageRcvd.msgId = Integer.parseInt(msgRecievedArray[3]);
                    messageRcvd.msgFromProcess = Integer.parseInt(msgRecievedArray[4]);
                    messageRcvd.deliverable = msgRecievedArray[5];
                    messageRcvd.msgText = msgRecievedArray[6];

                    if(messageRcvd.msgType.equals("M")){
                        messageRcvd.msgType = "P";
                        messageRcvd.msgPriority = ++proposed;
                        if(messageRcvd.msgFromProcess != procNum){
                            messageRcvd.msgFromProcess = procNum;
                            msgObjectList.add(messageRcvd);
                        }
                        Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                                Integer.parseInt(allRemotePorts[messageRcvd.msgProcNum]));
                        DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                        out.writeUTF(msgRecieved);
                        socket.close();
                    }
                    else if(messageRcvd.msgType.equals("P")){
                        for(MessageClass eachMsg: msgObjectList){
                            if(eachMsg.msgId == messageRcvd.msgId &&
                                    eachMsg.msgProcNum == messageRcvd.msgProcNum){
                                eachMsg.receipt += 1;

                                eachMsg.msgPriority = eachMsg.msgPriority > messageRcvd.msgPriority ?
                                        eachMsg.msgPriority : messageRcvd.msgPriority;
                            }
                            if(eachMsg.receipt == aliveNodes){
                                eachMsg.msgType = "R";
                                eachMsg.receipt = 0;
                                for(int i=0;i<allRemotePorts.length;i++){
                                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                                            Integer.parseInt(allRemotePorts[i]));
                                    DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                                    out.writeUTF(eachMsg.getToString() + "\n");
                                    socket.close();
                                }
                                break;
                            }
                        }
                    }
                    else if(messageRcvd.msgType.equals("R")){
                        proposed = messageRcvd.msgPriority > proposed ?
                                messageRcvd.msgPriority : proposed;
                        for(MessageClass eachMsg: msgObjectList){
                            if(eachMsg.msgId == messageRcvd.msgId &&
                                    eachMsg.msgProcNum == messageRcvd.msgProcNum){
                                eachMsg.msgPriority = messageRcvd.msgPriority;
                                eachMsg.deliverable = "Y";
                                Collections.sort(msgObjectList,MessageClass.priorityComparator);
                                break;
                            }
                        }
                    }
                    Iterator<MessageClass> iterator = msgObjectList.iterator();
                    while(iterator.hasNext()){
                        MessageClass tempMsgObject = iterator.next();
                        if(tempMsgObject.deliverable.equals("Y")){
                            publishProgress(tempMsgObject.msgText);
                            iterator.remove();
                        }
                        else {
                            break;
                        }
                    }
                }catch(IOException e) {

                }
            }while(!inSocket.isInputShutdown());
            return null;
        }

        protected void onProgressUpdate(String...strings) {
            /*
             * The following code displays what is received in doInBackground().
             */
            String strReceived = strings[0].trim();
            TextView remoteTextView = (TextView) findViewById(R.id.textView1);
            remoteTextView.append(strReceived + "\t\n");

            Uri uri = buildUri("content", "edu.buffalo.cse.cse486586.groupmessenger1.provider");

            ContentValues keyValueToInsert = new ContentValues();
            keyValueToInsert.put("key", Integer.toString(keyCount++));
            keyValueToInsert.put("value",strReceived);
            getContentResolver().insert(uri,keyValueToInsert);

            /*String filename = "SimpleMessengerOutput";
            String string = strReceived + "\n";
            FileOutputStream outputStream;

            try {
                outputStream = openFileOutput(filename, Context.MODE_PRIVATE);
                outputStream.write(string.getBytes());
                outputStream.close();
            } catch (Exception e) {
                Log.e(TAG, "File write failed");
            }*/

            return;
        }
    }

    private Uri buildUri(String scheme, String authority) {
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority(authority);
        uriBuilder.scheme(scheme);
        return uriBuilder.build();
    }
    /***
     * ClientTask is an AsyncTask that should send a string over the network.
     * It is created by ClientTask.executeOnExecutor() call whenever OnKeyListener.onKey() detects
     * an enter key press event.
     *
     * @author stevko
     *
     */
    private class ClientTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... msgs) {
            String msgToSend = msgs[0];
            try {
                String[] msgToSendArray = msgToSend.split("::");
                MessageClass message = new MessageClass();
                message.msgType = msgToSendArray[0];
                message.msgPriority = Integer.parseInt(msgToSendArray[1]);
                message.msgProcNum = Integer.parseInt(msgToSendArray[2]);
                message.msgId = Integer.parseInt(msgToSendArray[3]);
                message.msgFromProcess = Integer.parseInt(msgToSendArray[4]);
                message.deliverable = msgToSendArray[5];
                message.msgText = msgToSendArray[6];
                msgObjectList.add(message);

                for(int i=0; i<allRemotePorts.length;i++){
                    String remotePort = allRemotePorts[i];
                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(remotePort));
                    /** TODO: Fill in your client code that sends out a message.
                     */
                    DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                    out.writeUTF(msgToSend);
                    socket.close();

                }
            } catch (UnknownHostException e) {
                Log.e(TAG, "ClientTask UnknownHostException");
            } catch (IOException e) {
                Log.e(TAG, "ClientTask socket IOException");
            }

            return null;
        }
    }
}
