package com.goreecloud.launcher;
import android.content.Context;
import android.view.DragEvent;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Native drag destination showing the real Home pages and 5-row placement slots. */
public final class HomeDragOverlay {
    public interface Placement { boolean place(String appKey, int page, int slot); }
    private HomeDragOverlay() {}
    public static void show(Context context, FrameLayout layer,
                            WorkspaceState state, int columns, Placement placement) {
        layer.removeAllViews();
        layer.setBackgroundColor(0xF0182737);
        layer.setVisibility(View.VISIBLE);
        layer.setOnDragListener((v,event)->{
            if(event.getAction()==DragEvent.ACTION_DRAG_ENDED)layer.setVisibility(View.GONE);
            return true;
        });
        ScrollView scroller=new ScrollView(context);
        scroller.setFillViewport(true);
        scroller.setPadding(dp(context,22),dp(context,66),dp(context,22),dp(context,60));
        layer.addView(scroller,new FrameLayout.LayoutParams(-1,-1));
        LinearLayout body=new LinearLayout(context);
        body.setOrientation(LinearLayout.VERTICAL);
        scroller.addView(body);
        TextView heading=label(context,"Move app to Home",23);
        body.addView(heading);
        body.addView(label(context,"Hold and drop on a page slot • choose another page below",13));
        final int cols=Math.max(3,Math.min(6,columns));
        for(int page=0;page<state.pageCount();page++) {
            final int targetPage=page;
            TextView title=label(context,"Home page "+(page+1),16);
            LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(-1,-2);
            tp.topMargin=dp(context,16);body.addView(title,tp);
            GridLayout grid=new GridLayout(context);
            grid.setColumnCount(cols);
            grid.setRowCount(5);
            grid.setBackground(GoreeVisuals.card(context.getResources(),0xCC334A57,18));
            LinearLayout.LayoutParams gp=new LinearLayout.LayoutParams(-1,dp(context,190));
            body.addView(grid,gp);
            for(int i=0;i<5*cols;i++) {
                TextView placeholder=label(context,"·",16);
                placeholder.setGravity(Gravity.CENTER);
                placeholder.setBackground(GoreeVisuals.card(context.getResources(),0x703E5A6A,6));
                GridLayout.LayoutParams slot=new GridLayout.LayoutParams(
                    GridLayout.spec(i/cols,1f),GridLayout.spec(i%cols,1f));
                slot.width=0;slot.height=0;
                slot.setMargins(dp(context,2),dp(context,2),dp(context,2),dp(context,2));
                grid.addView(placeholder,slot);
            }
            grid.setOnDragListener((v,event)->{
                switch(event.getAction()){
                    case DragEvent.ACTION_DRAG_STARTED:
                        return event.getLocalState() instanceof String;
                    case DragEvent.ACTION_DROP:
                        Object stateKey=event.getLocalState();
                        if(!(stateKey instanceof String))return false;
                        int width=Math.max(1,grid.getWidth()),height=Math.max(1,grid.getHeight());
                        int c=Math.min(cols-1,Math.max(0,(int)(event.getX()*cols/width)));
                        int r=Math.min(4,Math.max(0,(int)(event.getY()*5/height)));
                        boolean success=placement.place((String)stateKey,targetPage,r*cols+c);
                        layer.setVisibility(View.GONE);
                        return success;
                    case DragEvent.ACTION_DRAG_ENDED:
                        layer.setVisibility(View.GONE);
                        return true;
                    default:return true;
                }
            });
        }
    }
    private static TextView label(Context ctx,String value,int size) {
        TextView t=new TextView(ctx);t.setText(value);t.setTextSize(size);
        t.setTextColor(0xFFEDF9FD);t.setPadding(dp(ctx,8),dp(ctx,7),dp(ctx,8),dp(ctx,7));
        return t;
    }
    private static int dp(Context c,int v){return Math.round(v*c.getResources().getDisplayMetrics().density);}
}
