package edu.buffalo.cse.cse486586.groupmessenger2;

import java.util.Arrays;
import java.util.UUID;

/**
 * Created by nikhil on 3/14/15.
 */
public class MsgDecisionObject {

    public UUID id;
    private int proposedSeqNum[] = {-1, -1, -1, -1, -1};
    private boolean decisionMade;
    private int finalSeq;
    private int finalPriority;

    public MsgDecisionObject(UUID id) {
        this.id = id;
        this.decisionMade = false;
        this.finalSeq = -1;
        this.finalPriority = -1;
    }

    public void setProposedSeqNum(int proposedSeqNum, int from) {
        this.proposedSeqNum[from] = proposedSeqNum;
    }

    public int getFinalSeq() {
        return finalSeq;
    }

    public int getFinalPriority() {
        return finalPriority;
    }

    @Override
    public String toString() {
        return "MsgDecisionObject{" +
                "id=" + id +
                ", proposedSeqNum=" + Arrays.toString(proposedSeqNum) +
                ", decisionMade=" + decisionMade +
                ", finalSeq=" + finalSeq +
                ", finalPriority=" + finalPriority +
                '}';
    }

    public boolean isDecisionMade() {
        return decisionMade;
    }

    public boolean makeDecision(boolean state[]) {

        //check if everyone replied
        for (int i = 0; i < proposedSeqNum.length; i++) {
            if (state[i] == true) {
                if (proposedSeqNum[i] == -1) {
                    return false;
                }
            }
        }

        int max = -1;
        int priority = -1;
        for (int i = 0; i < proposedSeqNum.length; i++) {
            if (this.proposedSeqNum[i] == -1) {
                continue;
            } else if (max < this.proposedSeqNum[i]) {
                max = this.proposedSeqNum[i];
                priority = i;
            } else if (max == this.proposedSeqNum[i]) {
                if (priority < i) {
                    priority = i;
                }
            }
        }
        this.finalSeq = max;
        this.finalPriority = priority;
        this.decisionMade = true;
        return true;
    }
}
