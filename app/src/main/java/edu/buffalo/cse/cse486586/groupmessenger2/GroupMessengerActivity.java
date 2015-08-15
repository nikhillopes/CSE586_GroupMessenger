package edu.buffalo.cse.cse486586.groupmessenger2;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 *
 * @author stevko
 */
public class GroupMessengerActivity extends Activity {

    static final String TAG = GroupMessengerActivity.class.getSimpleName();
    static final String[] REMOTE_PORTS = {"11108", "11112", "11116", "11120", "11124"};
    static final int SERVER_PORT = 10000;
    static final ReentrantLock lock = new ReentrantLock(true);
    int localSeq = -1, deliverySeq = -1, myFIFOCount = -1, myPortInt, myPriority;
    PriorityQueue<MsgQueueObject> messageDeliveryQueue = new PriorityQueue<>(100, new MsgQueueObject.MsgQueueObjectComparator());
    HashMap<UUID, String> messageStore = new HashMap<>();
    ArrayList<MsgDecisionObject> messageDecision = new ArrayList<>(20);
    boolean cleaned[] = {false, false, false, false, false};
    boolean[] state = {true, true, true, true, true};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);

        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);

        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        myPortInt = Integer.parseInt(portStr) * 2;
        for (int i = 0; i < REMOTE_PORTS.length; i++) {
            if (Integer.valueOf(REMOTE_PORTS[i].trim()) == myPortInt) {
                myPriority = i;
                Log.v(TAG, "MyPriority is " + Integer.toString(myPriority));
                break;
            }
        }

        Server MyServer = new Server();
        Thread serverThread = new Thread(MyServer);
        serverThread.start();
        Log.v(TAG, "Started server");

        ScheduledExecutorService pinger = Executors.newScheduledThreadPool(1);
        pinger.scheduleWithFixedDelay(new Ping(), 8, 5, TimeUnit.SECONDS);
        /*
         * TODO: Use the TextView to display your messageStore. Though there is no grading component
         * on how you display the messageStore, if you implement it, it'll make your debugging easier.
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
        final EditText editText = (EditText) findViewById(R.id.editText1);
        Button sendButton = (Button) findViewById(R.id.button4);

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (TextUtils.isEmpty(editText.getText())) {
                    Toast.makeText(getBaseContext(), "Please Enter Something", Toast.LENGTH_SHORT).show();
                } else {
                    String msg = editText.getText().toString() + "\n";
                    editText.setText(""); // This is one way to reset the input box.
                    TextView localTextView = (TextView) findViewById(R.id.textView1);
                    localTextView.append("\t" + msg); // This is one way to display a string.
                    TextView remoteTextView = (TextView) findViewById(R.id.textView2);
                    remoteTextView.append("\n");

                    new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg);
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }

    private Uri buildUri(String scheme, String authority) {
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority(authority);
        uriBuilder.scheme(scheme);
        return uriBuilder.build();
    }

    private void deliver(String strings, int finalSeq) {

        Log.v(TAG, "in delivery");
        final String strReceived = strings.trim();

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, "Updating UI");
                TextView remoteTextView = (TextView) findViewById(R.id.textView2);
                remoteTextView.append("\t" + strReceived);
                remoteTextView.append("\n");
                TextView localTextView = (TextView) findViewById(R.id.textView1);
                localTextView.append("\n");
            }
        });


        Uri mUri = buildUri("content", "edu.buffalo.cse.cse486586.groupmessenger2.provider");

        ContentValues mContentValues = new ContentValues();

        mContentValues.put("key", Integer.toString(finalSeq));
        mContentValues.put("value", strReceived);
        Uri newUri = getContentResolver().insert(mUri, mContentValues);
        if (newUri.equals(mUri) == true) {
            //Log.v(TAG, "Insert from Client Success");
        }
        mContentValues.clear();
        return;
    }

    private void clean() {
        for (int j = 0; j < state.length; j++) {
            if (state[j] == false && cleaned[j] == false) {
                cleaned[j] = true;
                Log.v(TAG, "CleanUP START");
                Log.v(TAG, "Cleaning Up MsgDecision");
                Log.v(TAG, "START........................................................");
                Iterator<MsgDecisionObject> displayIterator = messageDecision.iterator();
                while (displayIterator.hasNext()) {
                    Log.v(TAG, displayIterator.next().toString());
                }
                Log.v(TAG, "END........................................................");
                MsgDecisionObject tempArray2[] = new MsgDecisionObject[messageDecision.size()];
                messageDecision.toArray(tempArray2);
                for (int k = 0; k < tempArray2.length; k++) {
                    if (tempArray2[k].isDecisionMade() == false) {
                        tempArray2[k].setProposedSeqNum(-1, j);
                        boolean currentDecisionMade = tempArray2[k].makeDecision(state);
                        Log.v("TAG", "Making Decision = " + currentDecisionMade);

                        if (tempArray2[k].isDecisionMade() == true) {
                            Msg reply = new Msg(tempArray2[k].id, messageStore.get(tempArray2[k].id), tempArray2[k].getFinalSeq(), tempArray2[k].getFinalPriority(), 3, myPriority, -1);
                            Log.v(TAG, "Sent Type2 Reply= " + reply.toString());
                            for (int i = 0; i < REMOTE_PORTS.length; i++) {
                                Socket socket = null;
                                ObjectOutputStream out_temp = null;
                                try {
                                    if (state[i] == true) {
                                        socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(REMOTE_PORTS[i].trim()));
                                        out_temp = new ObjectOutputStream(socket.getOutputStream());
                                        out_temp.writeObject(reply);
                                        out_temp.flush();
                                        socket.close();
                                    }
                                } catch (IOException e) {
                                    Log.e(TAG, "ClientTask socket IOException, FATAL", e);
                                }
                            }
                        }
                    }
                }
                messageDecision.clear();
                Collections.addAll(messageDecision, tempArray2);
                Log.v(TAG, "START........................................................");
                displayIterator = messageDecision.iterator();
                while (displayIterator.hasNext()) {
                    Log.v(TAG, displayIterator.next().toString());
                }
                Log.v(TAG, "END........................................................");
                Log.v(TAG, "Cleaning up MsgQueue");

                Log.v(TAG, "START........................................................");
                Iterator<MsgQueueObject> displayIterator2 = messageDeliveryQueue.iterator();
                while (displayIterator2.hasNext()) {
                    Log.v(TAG, displayIterator2.next().toString());
                }
                Log.v(TAG, "END........................................................");

                MsgQueueObject tempArray[] = new MsgQueueObject[messageDeliveryQueue.size()];
                messageDeliveryQueue.toArray(tempArray);
                for (int i = 0; i < tempArray.length; i++) {
                    if (tempArray[i].getSource() == j) {
                        tempArray[i] = null;
                    } /*else if (tempArray[i].isDeliverable() && tempArray[i].getPriority()==j) {
                        tempArray[i].setDeliverable(false);
                        tempArray[i].setPriority(tempArray[i].getSource());
                        tempArray[i].setSeq(localSeq);
                    }*/
                }
                Arrays.sort(tempArray, new MsgQueueObject.MsgQueueObjectComparator());
                messageDeliveryQueue.clear();
                for (int i = 0; i < tempArray.length; i++) {
                    if (tempArray[i] != null) {
                        messageDeliveryQueue.add(tempArray[i]);
                    }
                }

                Log.v(TAG, "START........................................................");
                displayIterator2 = messageDeliveryQueue.iterator();
                while (displayIterator2.hasNext()) {
                    Log.v(TAG, displayIterator2.next().toString());
                }
                Log.v(TAG, "END........................................................");

                MsgQueueObject deliverableMsg = messageDeliveryQueue.peek();

                while (deliverableMsg != null && deliverableMsg.isDeliverable() == true) {
                    localSeq = Math.max(deliverableMsg.getSeq(), localSeq);
                    Log.v(TAG, "localSeqMAX=" + Integer.toString(localSeq));
                    Log.v(TAG, deliverableMsg.toString());
                    messageDeliveryQueue.poll();
                    deliverySeq++;
                    deliver(messageStore.get(deliverableMsg.id), deliverySeq);
                    deliverableMsg = messageDeliveryQueue.peek();
                }
                Log.v(TAG, "START........................................................");
                displayIterator2 = messageDeliveryQueue.iterator();
                while (displayIterator2.hasNext()) {
                    Log.v(TAG, displayIterator2.next().toString());
                }
                Log.v(TAG, "END........................................................");
                Log.v(TAG, "CleanUP END");
            }
        }
    }

    private class Ping implements Runnable {
        public void run() {
            Msg pingMessage = new Msg(null, null, -1, myPriority, -2, myPriority, -1);
            Log.v(TAG, "Sending Ping " + pingMessage.toString());
            for (int i = 0; i < REMOTE_PORTS.length; i++) {
                Socket socket = null;
                ObjectOutputStream out = null;
                try {
                    if (state[i] == true && i != myPriority) {
                        socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                                Integer.parseInt(REMOTE_PORTS[i].trim()));
                        out = new ObjectOutputStream(socket.getOutputStream());
                        out.writeObject(pingMessage);
                        out.flush();
                        out.close();
                        socket.close();
                        Log.v(TAG, "ping " + Integer.toString(i));
                    }
                } catch (UnknownHostException e) {
                    Log.e(TAG, "ClientTask UnknownHostException");
                } catch (IOException e) {
                    Log.e(TAG, "ClientTask socket IOException", e);
                    lock.lock();
                    state[i] = false;
                    Log.e(TAG, "Process Timed-out while sending ping " + (i) + " state set to false");
                    Log.e(TAG, "State = " + Arrays.toString(state));
                    clean();
                    Msg outFail = new Msg(null, null, -1, i, 0, myPriority, -1);
                    Log.e(TAG, "Informing others of Failure" + outFail.toString());
                    for (int j = 0; j < REMOTE_PORTS.length; j++) {
                        Socket socket2 = null;
                        ObjectOutputStream out2 = null;
                        try {
                            if (state[j] == true) {
                                socket2 = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                                        Integer.parseInt(REMOTE_PORTS[j].trim()));
                                out2 = new ObjectOutputStream(socket2.getOutputStream());
                                out2.writeObject(outFail);
                                out2.flush();
                                out2.close();
                                socket2.close();
                                Log.v(TAG, "Informing " + Integer.toString(j));
                            }

                        } catch (UnknownHostException f) {
                            Log.e(TAG, "ClientTask UnknownHostException");
                        } catch (IOException f) {
                            Log.e(TAG, "ClientTask socket IOException", f);
                        }
                    }
                    lock.unlock();
                }
            }
            return;
        }
    }

    private class Server implements Runnable {
        public void run() {

            try {
                ServerSocket serverSocket = new ServerSocket(SERVER_PORT);

                while (true) {
                    try {


                        Socket inSocket = serverSocket.accept();
                        ObjectInputStream in = new ObjectInputStream(inSocket.getInputStream());
                        lock.lock();
                        Msg inMsg = null;
                        inMsg = (Msg) in.readObject();

                        Log.v(TAG, "got message type=" + inMsg.msgType + " " + inMsg.toString());

                        if (inMsg.msgType == 0) {
                            if (state[inMsg.priority] == true) {
                                state[inMsg.priority] = false;
                                clean();
                            }
                            continue;
                        }
                        if (inMsg.msgType == -2) {
                            continue;
                        }
                        if (state[inMsg.source] == false) {
                            continue;
                        }

                        if (messageStore.containsKey(inMsg.id) == false) {
                            messageStore.put(inMsg.id, inMsg.msg);
                            Log.v(TAG, "added to message store");
                            boolean putSuccess = messageDeliveryQueue.add(new MsgQueueObject(inMsg.id, localSeq, inMsg.priority, false, inMsg.source, inMsg.FIFO));
                            Log.v(TAG, "added to messageDeliveryQueue " + Boolean.toString(putSuccess));
                            Log.v(TAG, "START........................................................");
                            Iterator<MsgQueueObject> displayIterator = messageDeliveryQueue.iterator();
                            while (displayIterator.hasNext()) {
                                Log.v(TAG, displayIterator.next().toString());
                            }
                            Log.v(TAG, "END........................................................");

                            if (inMsg.source == myPriority) {
                                putSuccess = messageDecision.add(new MsgDecisionObject(inMsg.id));
                                Log.v(TAG, "added to messageDecisionQueue " + Boolean.toString(putSuccess));
                                Log.v(TAG, "START........................................................");
                                Iterator<MsgDecisionObject> displayIterator2 = messageDecision.iterator();
                                while (displayIterator2.hasNext()) {
                                    Log.v(TAG, displayIterator2.next().toString());
                                }
                                Log.v(TAG, "END........................................................");
                            }
                        }

                        if (inMsg.msgType == 1) {
                            localSeq++;
                            Log.v(TAG, "localSeq++=" + Integer.toString(localSeq));
                            //handle new message, choose a local seq num and reply back
                            //Build Reply
                            Msg reply = new Msg(inMsg.id, inMsg.msg, localSeq, myPriority, 2, inMsg.source, inMsg.FIFO);
                            //Send reply & increment counter
                            try {
                                Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(REMOTE_PORTS[inMsg.priority].trim()));
                                ObjectOutputStream out_temp = new ObjectOutputStream(socket.getOutputStream());
                                out_temp.writeObject(reply);
                                out_temp.flush();
                                socket.close();

                                //Log
                                Log.v(TAG, "Sent Type1 Reply=" + reply.toString());
                            } catch (SocketException e) {
                                Log.e(TAG, "ClientTask socket IOException, could not reply final seq", e);
                                state[inMsg.priority] = false;
                                Log.e(TAG, "Process Timed-out while sending type1 reply" + (inMsg.priority) + " state set to false");
                                Log.e(TAG, "State = " + Arrays.toString(state));
                                clean();
                                Msg outFail = new Msg(null, null, -1, inMsg.priority, 0, myPriority, -1);
                                Log.e(TAG, "Informing others of Failure" + outFail.toString());
                                for (int j = 0; j < REMOTE_PORTS.length; j++) {
                                    Socket socket2 = null;
                                    ObjectOutputStream out2 = null;
                                    try {
                                        if (state[j] == true) {
                                            socket2 = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                                                    Integer.parseInt(REMOTE_PORTS[j].trim()));
                                            out2 = new ObjectOutputStream(socket2.getOutputStream());
                                            out2.writeObject(outFail);
                                            out2.flush();
                                            out2.close();
                                            socket2.close();
                                            Log.v(TAG, "Informing " + Integer.toString(j));
                                        }

                                    } catch (UnknownHostException f) {
                                        Log.e(TAG, "ClientTask UnknownHostException");
                                    } catch (IOException f) {
                                        Log.e(TAG, "ClientTask socket IOException", f);
                                    }
                                }
                            }

                        } else if (inMsg.msgType == 2) {
                            Log.v(TAG, "processing type 2 from " + Integer.toString(inMsg.priority) + " seqProposed = " + Integer.toString(inMsg.seq) + " for message ID = " + inMsg.id.toString());
                            //handle reply by other processes, i get to decide final seq num
                            Log.v(TAG, "START........................................................");
                            Iterator<MsgDecisionObject> displayIterator = messageDecision.iterator();
                            while (displayIterator.hasNext()) {
                                Log.v(TAG, displayIterator.next().toString());
                            }
                            Log.v(TAG, "END........................................................");
                            MsgDecisionObject currentDecision = null;
                            Iterator<MsgDecisionObject> decisionIterator = messageDecision.iterator();
                            while (decisionIterator.hasNext()) {
                                currentDecision = decisionIterator.next();
                                if (currentDecision.id.equals(inMsg.id)) {
                                    currentDecision.setProposedSeqNum(inMsg.seq, inMsg.priority);
                                    Log.v(TAG, "updated proposed seq ");
                                    boolean currentDecisionMade = currentDecision.makeDecision(state);
                                    Log.v("TAG", "Making Decision = " + currentDecisionMade);
                                    decisionIterator.remove();
                                    messageDecision.add(currentDecision);
                                    break;
                                }
                            }
                            displayIterator = messageDecision.iterator();
                            while (displayIterator.hasNext()) {
                                Log.v(TAG, displayIterator.next().toString());
                            }
                            if (currentDecision != null && currentDecision.isDecisionMade() == true) {


                                Msg reply = new Msg(inMsg.id, inMsg.msg, currentDecision.getFinalSeq(), currentDecision.getFinalPriority(), 3, inMsg.source, inMsg.FIFO);
                                Log.v(TAG, "Sent Type2 Reply= " + reply.toString());

                                for (int i = 0; i < REMOTE_PORTS.length; i++) {
                                    Socket socket = null;
                                    ObjectOutputStream out_temp = null;
                                    try {
                                        if (state[i] == true) {
                                            socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(REMOTE_PORTS[i].trim()));
                                            out_temp = new ObjectOutputStream(socket.getOutputStream());
                                            out_temp.writeObject(reply);
                                            out_temp.flush();
                                            socket.close();
                                        }

                                    } catch (IOException e) {
                                        Log.e(TAG, "ClientTask socket IOException, could not reply final seq", e);
                                        state[i] = false;
                                        out_temp.flush();
                                        socket.close();
                                        Log.e(TAG, "Process Timed-out while sending type2 reply" + (i) + " state set to false");
                                        Log.e(TAG, "State = " + Arrays.toString(state));
                                        clean();
                                        Msg outFail = new Msg(null, null, -1, i, 0, myPriority, -1);
                                        Log.e(TAG, "Informing others of Failure" + outFail.toString());
                                        for (int j = 0; j < REMOTE_PORTS.length; j++) {
                                            Socket socket2 = null;
                                            ObjectOutputStream out2 = null;
                                            try {
                                                if (state[j] == true) {
                                                    socket2 = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                                                            Integer.parseInt(REMOTE_PORTS[j].trim()));
                                                    out2 = new ObjectOutputStream(socket2.getOutputStream());
                                                    out2.writeObject(outFail);
                                                    out2.flush();
                                                    out2.close();
                                                    socket2.close();
                                                    Log.v(TAG, "Informing " + Integer.toString(j));
                                                }

                                            } catch (UnknownHostException f) {
                                                Log.e(TAG, "ClientTask UnknownHostException");
                                            } catch (IOException f) {
                                                Log.e(TAG, "ClientTask socket IOException", f);
                                            }
                                        }
                                    }
                                }
                            }

                        } else if (inMsg.msgType == 3) {
                            //this is the final seq num, take appropriate action
                            //add to queue if out of order; when ordered deliver in order and display
                            Log.v(TAG, "START........................................................");
                            Iterator<MsgQueueObject> displayIterator = messageDeliveryQueue.iterator();
                            while (displayIterator.hasNext()) {
                                Log.v(TAG, displayIterator.next().toString());
                            }
                            Log.v(TAG, "END........................................................");

                            Iterator<MsgQueueObject> updateMessageIterator = messageDeliveryQueue.iterator();
                            while (updateMessageIterator.hasNext()) {
                                MsgQueueObject current = updateMessageIterator.next();
                                if (current.getId().equals(inMsg.id)) {
                                    current.setDeliverable(true);
                                    current.setPriority(inMsg.priority);
                                    current.setSeq(inMsg.seq);
                                    MsgQueueObject tempArray[] = new MsgQueueObject[messageDeliveryQueue.size()];
                                    messageDeliveryQueue.toArray(tempArray);
                                    Arrays.sort(tempArray, new MsgQueueObject.MsgQueueObjectComparator());
                                    messageDeliveryQueue.clear();
                                    Collections.addAll(messageDeliveryQueue, tempArray);
                                    break;
                                }
                            }

                            Log.v(TAG, "START........................................................");
                            displayIterator = messageDeliveryQueue.iterator();
                            while (displayIterator.hasNext()) {
                                Log.v(TAG, displayIterator.next().toString());
                            }
                            Log.v(TAG, "END........................................................");


                            MsgQueueObject deliverableMsg = messageDeliveryQueue.peek();

                            while (deliverableMsg != null && deliverableMsg.isDeliverable() == true) {
                                localSeq = Math.max(deliverableMsg.getSeq(), localSeq);
                                Log.v(TAG, "localSeqMAX=" + Integer.toString(localSeq));
                                Log.v(TAG, deliverableMsg.toString());
                                messageDeliveryQueue.poll();
                                deliverySeq++;
                                deliver(messageStore.get(deliverableMsg.id), deliverySeq);
                                deliverableMsg = messageDeliveryQueue.peek();
                            }
                            Log.v(TAG, "START........................................................");
                            displayIterator = messageDeliveryQueue.iterator();
                            while (displayIterator.hasNext()) {
                                Log.v(TAG, displayIterator.next().toString());
                            }
                            Log.v(TAG, "END........................................................");

                        }
                        inSocket.close();
                        in.close();
                    } catch (SocketTimeoutException e) {
                        Log.e(TAG, "Timeout Occurred", e);
                    } finally {
                        lock.unlock();
                    }
                }

            } catch (IOException e) {
                Log.e(TAG, "Can't accept connection", e);
            } catch (ClassNotFoundException e) {
                Log.e(TAG, "Class Not Fount Exeception", e);
            }
        }
    }

    private class ClientTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... msgs) {
            String msgString = msgs[0].trim();
            myFIFOCount++;
            Msg outMessage = new Msg(UUID.randomUUID(), msgString, -1, myPriority, 1, myPriority, myFIFOCount);
            Log.v(TAG, "Sending Initail msg " + outMessage.toString());
            for (int i = 0; i < REMOTE_PORTS.length; i++) {
                Socket socket = null;

                ObjectOutputStream out = null;
                try {

                    if (state[i] == true) {
                        socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                                Integer.parseInt(REMOTE_PORTS[i].trim()));
                        out = new ObjectOutputStream(socket.getOutputStream());
                        out.writeObject(outMessage);
                        out.flush();
                        out.close();
                        socket.close();
                        Log.v(TAG, "sending to" + Integer.toString(i));
                    }

                } catch (UnknownHostException e) {
                    Log.e(TAG, "ClientTask UnknownHostException");
                } catch (IOException e) {
                    Log.e(TAG, "ClientTask socket IOException", e);
                    lock.lock();
                    state[i] = false;
                    Log.e(TAG, "Process Timed-out while sending initialMsg " + (i) + " state set to false");
                    Log.e(TAG, "State = " + Arrays.toString(state));
                    clean();
                    Msg outFail = new Msg(null, null, -1, i, 0, myPriority, -1);
                    Log.e(TAG, "Informing others of Failure" + outFail.toString());
                    for (int j = 0; j < REMOTE_PORTS.length; j++) {
                        Socket socket2 = null;
                        ObjectOutputStream out2 = null;
                        try {
                            if (state[j] == true) {
                                socket2 = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                                        Integer.parseInt(REMOTE_PORTS[j].trim()));
                                out2 = new ObjectOutputStream(socket2.getOutputStream());
                                out2.writeObject(outFail);
                                out2.flush();
                                out2.close();
                                socket2.close();
                                Log.v(TAG, "Informing " + Integer.toString(j));
                            }

                        } catch (UnknownHostException f) {
                            Log.e(TAG, "ClientTask UnknownHostException");
                        } catch (IOException f) {
                            Log.e(TAG, "ClientTask socket IOException", f);
                        }
                    }
                    lock.unlock();
                }
            }
            return null;
        }
    }//end client task
}
