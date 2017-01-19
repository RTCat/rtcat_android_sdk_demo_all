package com.shishimao.android.demo.white_board;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;


public class PaintView extends View {
    private static final String TAG = "PaintView";
    private Bitmap bitmap;
    private Canvas canvas;

    private Path penPath;
    private Path otherPenPath;
    private Paint penPaint;
    private Paint otherPaint;

    private Paint canvasPaint;

    private final String startPoint = "start point";
    private final String endPoint = "end point";
    private final String middlePoint = "middle point";
    private int pointNum = 0;
    private int lineNum = 0;

    private MainActivity mainActivity;

    public PaintView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        setupPainting();
    }

    public void setAct(MainActivity mainActivity){
        this.mainActivity = mainActivity;
    }

    public void clearCanvas() {
        this.canvas.drawColor(0, PorterDuff.Mode.CLEAR);
        invalidate();
    }

    protected void setupPainting() {
        this.bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
        this.canvas = new Canvas(bitmap);

        this.penPath = new Path();
        this.otherPenPath = new Path();

        this.penPaint = new Paint();
        this.penPaint.setColor(Color.BLUE);
        this.penPaint.setAntiAlias(true);
        this.penPaint.setStrokeWidth(20);
        this.penPaint.setStyle(Paint.Style.STROKE);
        this.penPaint.setStrokeJoin(Paint.Join.ROUND);
        this.penPaint.setStrokeCap(Paint.Cap.ROUND);

        this.otherPaint = new Paint();
        this.otherPaint.setColor(Color.GREEN);
        this.otherPaint.setAntiAlias(true);
        this.otherPaint.setStrokeWidth(20);
        this.otherPaint.setStyle(Paint.Style.STROKE);
        this.otherPaint.setStrokeJoin(Paint.Join.ROUND);
        this.otherPaint.setStrokeCap(Paint.Cap.ROUND);

        this.canvasPaint = new Paint(Paint.DITHER_FLAG);
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);

        this.bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        this.canvas = new Canvas(bitmap);
    }

    @Override
    protected void onDraw(Canvas cvs) {
        cvs.drawBitmap(bitmap, 0, 0, canvasPaint);
        cvs.drawPath(penPath, penPaint);
        cvs.drawPath(otherPenPath,otherPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        PointF touchPoint = new PointF();
        touchPoint.set(event.getX(), event.getY());

        //pointNum++;
        switch(event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                pointNum = 0;
                lineNum++;
                this.penPath.moveTo(touchPoint.x, touchPoint.y);
                sendToOthers(touchPoint.x,touchPoint.y,startPoint,pointNum,lineNum);
                break;

            case MotionEvent.ACTION_MOVE:
                pointNum ++;
                this.penPath.lineTo(touchPoint.x, touchPoint.y);
                sendToOthers(touchPoint.x,touchPoint.y,middlePoint,pointNum,lineNum);
                break;

            case MotionEvent.ACTION_UP:
                this.canvas.drawPath(penPath, penPaint);
                pointNum++;
                sendToOthers(touchPoint.x,touchPoint.y,endPoint,pointNum,lineNum);
                this.penPath.reset();
                break;

            default:
                return false;
        }
        invalidate();
        return true;
    }

    public void sendToOthers(float x,float y,String point,int pointNum,int lineNum){
        try {
            JSONObject paintDot = new JSONObject();
            paintDot.put("x",(double) x/getWidth());
            paintDot.put("y",(double) y/getHeight());
            paintDot.put("point",point);
            paintDot.put("pointNum",pointNum);
            paintDot.put("lineNum",lineNum);
            mainActivity.broadcastMessage(paintDot.toString());
            Log.i(TAG,"Send to others: " + paintDot.toString());
        }catch (Exception e){
            Log.e("E",e.getMessage());
        }
    }

    public void receiveFromOthers(String msg){
        Log.i(TAG,msg);
        try{
            JSONObject jsonObject = new JSONObject(msg);
            double x = jsonObject.getDouble("x")*getWidth();
            double y = jsonObject.getDouble("y")*getHeight();
            String point = jsonObject.getString("point");
            int pointNum = jsonObject.getInt("pointNum");
            int lineNum = jsonObject.getInt("lineNum");

            drawLine((float)x,(float)y ,point, pointNum,lineNum);
        }catch (JSONException e){
            Log.e(TAG,e.getMessage());
        }
    }

    ArrayList<PointJ> pointJs= new ArrayList<>();
    int receiveLineNum = 0;
    boolean isReceive = false;

    public void drawLine(float x,float y,String point,int pointNum,int lineNum){
        if(point.equals(startPoint)){
            receiveLineNum = lineNum;
            isReceive = true;
        }

        if(isReceive)
            if(lineNum == receiveLineNum)
                pointJs.add(new PointJ(x,y,point,pointNum,lineNum));

            if(point.equals((endPoint))){
                isReceive = false;

                receiveLineNum++;

                Collections.sort(pointJs);
                //draw
                for (PointJ pointJ:pointJs
                     ) {
                    drawPoint(pointJ.x,pointJ.y,pointJ.point);
                }
                pointJs.clear();
                //this.pointNum = 0;

            }


    }

    public void drawPoint(float x, float y,String point) {
        switch (point) {
            case startPoint:
                this.otherPenPath.moveTo(x, y);
                break;
            case middlePoint:
                this.otherPenPath.lineTo(x, y);
                break;
            case endPoint:
                this.canvas.drawPath(otherPenPath, otherPaint);
                this.otherPenPath.reset();
                break;
        }
        invalidate();
    }


    class PointJ implements Comparable<PointJ>{
        float x;
        float y;
        String point;
        int pointNum;
        int lineNum;
        PointJ(float x,float y,String point,int pointNum,int lineNum){
            this.x = x;
            this.y = y;
            this.point = point;
            this.pointNum = pointNum;
            this.lineNum = lineNum;
        }

        @Override
        public int compareTo(PointJ p){
            return this.pointNum > p.pointNum ? 1 : (this.pointNum < p.pointNum ? -1 : 0);
        }

    }

}

