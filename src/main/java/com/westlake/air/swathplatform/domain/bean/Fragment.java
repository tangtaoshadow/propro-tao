package com.westlake.air.swathplatform.domain.bean;

import lombok.Data;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-06-12 20:27
 */
@Data
public class Fragment implements Comparable<Fragment>{

    String transitionId;

    String sequence;

    String type;

    int charge;

    int adjust;

    double monoWeight;

    double averageWeight;

    int count = 1;

    public Fragment(){}

    public Fragment(String transitionId){
        this.transitionId = transitionId;
    }

    public void count(){
        this.count++;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == null){
            return false;
        }

        if(obj instanceof Fragment){
            Fragment fragment = (Fragment) obj;
            if(sequence == null || type == null || fragment.getSequence() == null || fragment.getType() == null){
                return false;
            }

            return (this.sequence.equals(fragment.getSequence()) && this.type.equals(fragment.getType()));
        }else{
            return false;
        }

    }

    @Override
    public int hashCode() {
        return (sequence+type).hashCode();
    }

    @Override
    public int compareTo(Fragment o) {
        if(o == null){
            return 1;
        }
        if(this.getCount() > o.getCount()){
            return -1;
        }else if(this.getCount() == o.getCount()){
            return 0;
        }else{
            return 1;
        }
    }

//    @Override
//    public int compareTo(Fragment fragment) {
//        if(this.count > fragment.count){
//            return 1;
//        }else{
//            return -1;
//        }
//    }
}
