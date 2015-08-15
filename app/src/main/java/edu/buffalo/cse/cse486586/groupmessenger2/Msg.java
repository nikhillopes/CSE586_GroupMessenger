package edu.buffalo.cse.cse486586.groupmessenger2;

import java.io.Serializable;
import java.util.UUID;

/**
 * Created by nikhil on 2/25/15.
 */
public class Msg implements Serializable {

    UUID id;
    String msg;
    int seq;
    int priority;
    int msgType;
    int source;
    int FIFO;

    public Msg(UUID id, String msg, int seq, int priority, int msgType, int source, int FIFO) {

        this.id = id;
        this.msg = msg;
        this.seq = seq;
        this.priority = priority;
        this.msgType = msgType;
        this.source=source;
        this.FIFO=FIFO;
    }

    @Override
    public String toString() {
        return "Msg{" +
                "id=" + id +
                ", msg='" + msg + '\'' +
                ", seq=" + seq +
                ", priority=" + priority +
                ", msgType=" + msgType +
                ", source=" + source +
                ", FIFO=" + FIFO +
                '}';
    }

    public Msg() {
    }
}
