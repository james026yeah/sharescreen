package com.archermind.ashare.network;


public class Session {
	private int mSessionId;
	private static int sSessionCount = 0;
	public Session() {		
		mSessionId = sSessionCount;
		++sSessionCount;
	}	
	public Session(int sessionId) {
		mSessionId = sessionId;
	}	
	public int getSessionId() {
		return mSessionId;
	}

	@Override
	public boolean equals(Object obj) {
		return (obj instanceof Session) ? (((Session)obj).mSessionId == mSessionId) : false;
	}	
	@Override
	public int hashCode() {
		return mSessionId;
	}	
}
