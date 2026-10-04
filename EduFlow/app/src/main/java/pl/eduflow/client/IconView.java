package pl.eduflow.client;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

public final class IconView extends View {
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int kind;
    private int tint;
    public IconView(Context c, int kind, int tint) { super(c); this.kind=kind; this.tint=tint; }
    public void tint(int color) { tint=color; invalidate(); }
    @Override protected void onDraw(Canvas raw) {
        super.onDraw(raw);
        int checkpoint=raw.save();
        raw.translate((getWidth()-getHeight())/2f,0);
        raw.scale(getHeight()/24f,getHeight()/24f);
        p.setColor(tint); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(1.8f); p.setStrokeCap(Paint.Cap.ROUND); p.setStrokeJoin(Paint.Join.ROUND);
        if(kind==0) {
            Path a=new Path(); a.moveTo(3,10);a.lineTo(12,3);a.lineTo(21,10);a.moveTo(5,9);a.lineTo(5,21);a.lineTo(10,21);a.lineTo(10,14);a.lineTo(14,14);a.lineTo(14,21);a.lineTo(19,21);a.lineTo(19,9);raw.drawPath(a,p);
        } else if(kind==1) {
            raw.drawRoundRect(5,3,19,21,2,2,p);raw.drawLine(9,3,9,21,p);raw.drawLine(12,8,16,8,p);raw.drawLine(12,12,16,12,p);
        } else if(kind==2) {
            raw.drawRoundRect(3,5,21,21,3,3,p);raw.drawLine(3,10,21,10,p);raw.drawLine(8,3,8,7,p);raw.drawLine(16,3,16,7,p);p.setStyle(Paint.Style.FILL);raw.drawCircle(8,14,1,p);raw.drawCircle(12,14,1,p);raw.drawCircle(16,14,1,p);raw.drawCircle(8,18,1,p);raw.drawCircle(12,18,1,p);
        } else if(kind==3) {
            Path a=new Path();a.moveTo(6,18);a.lineTo(3,21);a.lineTo(3,6);a.quadTo(3,3,6,3);a.lineTo(18,3);a.quadTo(21,3,21,6);a.lineTo(21,15);a.quadTo(21,18,18,18);a.close();raw.drawPath(a,p);raw.drawLine(7,8,17,8,p);raw.drawLine(7,12,14,12,p);
        } else {
            p.setStyle(Paint.Style.FILL);raw.drawCircle(5,12,2,p);raw.drawCircle(12,12,2,p);raw.drawCircle(19,12,2,p);
        }
        raw.restoreToCount(checkpoint);
    }
}
