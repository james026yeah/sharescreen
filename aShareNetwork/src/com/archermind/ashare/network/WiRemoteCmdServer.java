package com.archermind.ashare.network;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

public final class WiRemoteCmdServer {
	private final static String TAG = "WiRemoteCmdServer";
	private final static int MAX_CLIENT_COUNTS = 1;
	private final static int MSG_ON_MESSAGE_ARRIVED = 1;
	private final static int MSG_ON_SESSION_CLOSED = 2;
	private static final int SERVER_PORT = 9999;
	private static WiRemoteCmdServer sInstance = null;
	private ServerSocket mServerSocket = null;
	private boolean mStarted = false;
	private HashMap<Session, ClientThread> mThreadPool = new HashMap<Session, ClientThread>();
	private Object mThreadPoolLock = new Object();
	private ServerSocketThread mListenerThread = null;
	private Handler mHandler = null;
	private onWiRemoteCmdListener mListener = null;
	private Handler.Callback mCb = new Handler.Callback() {
		@Override
		public boolean handleMessage(Message msg) {
			switch(msg.what) {
			case MSG_ON_MESSAGE_ARRIVED:
				onMessageArrived(msg);
				break;
			case MSG_ON_SESSION_CLOSED:
				onSessionClosed(msg);
				break;
			default:
				break;
			}
			return false;
		}		
	};
	
	public interface onWiRemoteCmdListener {
		public void onConnectAp(int sessionId, String ssid, String password);
		public void onDisconnectWifi(int sessionId);
		public void onOTAUpdate(int sessionId);
		public void onRenameDevice(int sessionId, String newName);
	}
	
	public void setOnWiRemoteCmdListener(onWiRemoteCmdListener ls) {
		mListener = ls;
	}	

	public boolean sendComand(int sessionId, String message) {		
		boolean bSend = false;		
		ClientThread thread = mThreadPool.get(new Session(sessionId));
		if(thread != null) {
			thread.sendMessage(message);
			bSend = true;
		}		
		return bSend;
	}
	
	private void onMessageArrived(Message msg) {	
		int sessionId = msg.arg1;
		WiRemoteCmdParser cmdParser = new WiRemoteCmdParser((String)msg.obj);
		Log.v(TAG, "on received message:" + (String)msg.obj);
		if(mListener == null) return;
		int command = cmdParser.getInt(Commands.KEY_CMD, Commands.CMD_INVALID);
		switch(command) {
		case Commands.CMD_STICK_CONNECT_AP_REQ:
			mListener.onConnectAp(sessionId, cmdParser.getString(Commands.KEY_AP_SSID), 
					cmdParser.getString(Commands.KEY_AP_PASSWD));
			break;
		case Commands.CMD_STICK_DISCONNECT_WIFI:
			mListener.onDisconnectWifi(sessionId);
			break;
		case Commands.CMD_STICK_OTA:
			mListener.onOTAUpdate(sessionId);
			break;
		case Commands.CMD_STICK_RENAME_RENDERER:
			break;
		default:
			break;
		}
		
	}
	
	private void onSessionClosed(Message msg) {
		Session closedSession = new Session(msg.arg1);//(Session)msg.obj;
		Log.v(TAG, "onSessionClosed before close mThreadPool size:" + mThreadPool.size());
		// Executed in UI thread
		synchronized(mThreadPoolLock) {
			mThreadPool.remove(closedSession);
		}
		Log.v(TAG, "onSessionClosed after close mThreadPool size:" + mThreadPool.size());
	}
	
	public static WiRemoteCmdServer getInstance() {
		if(sInstance == null) {
			sInstance = new WiRemoteCmdServer();
		}
		return sInstance;
	}
	
	public void start() {
		Log.v(TAG, "start >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
		if(!mStarted) {
			Log.v(TAG, "do start >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
			mStarted = true;
			prepare();
		}
	}
	
	public void stop() {
		Log.v(TAG, "stop <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
		if(mStarted) {
			Log.v(TAG, "do real stop <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
			if(mListenerThread != null) {
				mListenerThread.quit();
				mListenerThread = null;
			}
			// Executed in UI thread
			synchronized(mThreadPoolLock) {
				Iterator<Entry<Session, ClientThread>> iter = mThreadPool.entrySet().iterator(); 
				while (iter.hasNext()) { 
				    Entry<Session, ClientThread> entry = iter.next(); 
				    ClientThread thread = entry.getValue();
				    thread.quit();
				}
			}
			mStarted = false;
		}
	}
	
	private WiRemoteCmdServer() {
		mHandler = new Handler(mCb);
	}
	
	private void prepare() {
		try {
			mServerSocket = new ServerSocket(SERVER_PORT);
			mListenerThread = new ServerSocketThread();
			mListenerThread.start();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}	
	
	
	
	protected class ServerSocketThread extends Thread {
		public void quit() {
			if(mServerSocket != null) {
				try {
					mServerSocket.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		public void run() {
			Log.d(TAG, "ServerSocketThread is running");
			while (true) {				
				Socket client;				
				try {
					client = mServerSocket.accept();
					Log.d(TAG, "ListenThread mRemoteSocket: "
							+ client.getInetAddress());
					// Executed in Server socket thread
					synchronized(mThreadPoolLock) {
						ClientThread clientThread;
						Session session = new Session();
						clientThread = new ClientThread(client, session);
						clientThread.start();
						mThreadPool.put(session, clientThread);
						if(mThreadPool.size() > MAX_CLIENT_COUNTS) {
							//clientThread.sendMessage(WiRemoteCmdComposer);
							String message = WiRemoteCmdComposer.obtainServerConnErrorCmd(
									Commands.CONNECT_ERROR_EXCEED_MAX_CLIENT_COUNT);
							sendMessage(clientThread, message);
							client.close();
							client = null;
						}
					}
				} catch (IOException e) {
					e.printStackTrace();
					break;
				} catch (NullSocketException e) {
					e.printStackTrace();
					break;
				}  
			}

			mServerSocket = null;
			Log.v(TAG, "ServerSocketThread is quiting");
		}
	}
	

	protected void sendMessage(ClientThread thread, String message) {
		if(thread != null && message != null) {
			thread.sendMessage(message);
		}
	}
	
	
	protected class ClientThread extends Thread {
		private Socket mClient = null;
		private Session mSession = null;
		private BufferedReader mReader = null;
		private PrintWriter mWriter = null;
		public ClientThread(Socket client, Session session) throws NullSocketException {
			super();
			if(client == null) {
				throw new NullSocketException();
			}
			mClient = client;
			mSession = session;
			try {
				mReader = new BufferedReader(new InputStreamReader(client.getInputStream()));
				mWriter = new PrintWriter(new BufferedWriter(
						new OutputStreamWriter(client.getOutputStream())), true);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}		
		
		public void sendMessage(String message) {
			Log.v(TAG, "sendMessage--> " + message);
			if (mWriter != null) {
				mWriter.println(message);
			}
		}
		
		public void quit() {
			if(mClient != null) {
				try {
					mClient.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		public void run() {
			Log.v(TAG, "Session [" + mSession.getSessionId() + "] ClientThread is running");
			if(mClient == null) {
				Log.e(TAG, "Socket is null!!");
				return;
			}
			String message = null;			
			while (true) {
				try {
					if (mClient.isConnected() && !mClient.isInputShutdown()) {
						Log.v(TAG, "mClient.isConnected():" + mClient.isConnected() + 
								"mClient.isInputShutdown():" + mClient.isInputShutdown());
						message = mReader.readLine();
						if(message == null) {
							Log.v(TAG, "reveive null message");
							break;
						}						
						Log.v(TAG, "Read Message: " + message);
						if (message != null && mHandler != null) {
							Message msg = Message.obtain(null, MSG_ON_MESSAGE_ARRIVED, message);
							msg.arg1 = mSession.getSessionId();
							mHandler.sendMessage(msg);
						}
					}
				} catch (Exception e) {
					Log.e(TAG, e.toString());
					e.printStackTrace();
					break;
				}
			}
			
			Log.v(TAG, "Session [" + mSession.getSessionId() + "] ClientThread is exiting");
			try {	
				mReader.close();
				mWriter.close();
				mReader = null;
				mWriter = null;
				mClient = null;
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			Message msg = Message.obtain(null, MSG_ON_SESSION_CLOSED);
			//msg.obj = mSession;
			msg.arg1 = mSession.getSessionId();
			mHandler.sendMessage(msg);
		}
	}
}
