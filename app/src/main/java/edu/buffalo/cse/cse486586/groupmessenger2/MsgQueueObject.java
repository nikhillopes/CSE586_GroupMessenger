package edu.buffalo.cse.cse486586.groupmessenger2;

import java.util.Comparator;
import java.util.UUID;

/**
 * Created by nikhil on 3/14/15.
 */
public class MsgQueueObject {

    public UUID id;
    private int seq;
    private int priority;
    private boolean deliverable;
    private int source;
    private int FIFO;

    public MsgQueueObject(UUID id, int seq, int priority, boolean deliverable, int source, int FIFO) {
        this.id = id;
        this.seq = seq;
        this.priority = priority;
        this.deliverable = deliverable;
        this.source = source;
        this.FIFO=FIFO;
    }

    public void setSeq(int seq) {
        this.seq = seq;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public void setDeliverable(boolean deliverable) {
        this.deliverable = deliverable;
    }

    public UUID getId() {
        return id;
    }

    public MsgQueueObject(UUID id) {
        this.id = id;
    }

    public int getSeq() {
        return seq;
    }

    public int getFIFO() {
        return FIFO;
    }

    public int getPriority() {
        return priority;
    }

    public int getSource() {
        return source;
    }


    public boolean isDeliverable() {
        return deliverable;
    }


/*    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MsgQueueObject that = (MsgQueueObject) o;

        if (!id.equals(that.id)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }*/

    public static class MsgQueueObjectComparator implements Comparator<MsgQueueObject> {
        @Override
        public int compare(MsgQueueObject lhs, MsgQueueObject rhs) {
            if(lhs==null){
                return 1;
            } else if(rhs==null){
                return -1;
            }
            int compare = Integer.compare(lhs.getSeq(), rhs.getSeq());
            if (compare == 0) {
                compare = Boolean.compare(lhs.isDeliverable(), rhs.isDeliverable());
                if (compare == 0) {
                    compare = Integer.compare(rhs.getPriority(), lhs.getPriority());
                    return compare;
                } else {
                    return compare;
                }
            } else {
                return compare;
            }
        }
    }

    @Override
    public String toString() {
        return "MsgQueueObject{" +
                "id=" + id +
                ", seq=" + seq +
                ", priority=" + priority +
                ", deliverable=" + deliverable +
                ", source=" + source +
                ", FIFO=" + FIFO +
                '}';
    }
}
