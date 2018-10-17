package com.example.michael.sceneformfurniture;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.ar.core.Anchor;
import com.google.ar.core.Camera;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.Pose;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.HitTestResult;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.Scene;
import com.google.ar.sceneform.Sun;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.Renderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    private ArFragment fragment;
    private Uri selectedObject;
    private boolean deleteEnabled;
    private Set<Pose> poseList;
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        deleteEnabled=false;
        fragment=(ArFragment)getSupportFragmentManager().findFragmentById(R.id.sceneform_fragment);
        initializeGallery(fragment);
        fragment.setOnTapArPlaneListener((HitResult hitResult, Plane plane, MotionEvent motionEvent)->{
            if(plane.getType()!= Plane.Type.HORIZONTAL_UPWARD_FACING){return;}
                if(selectedObject!=null){
                    Anchor anchor = hitResult.createAnchor();
                    placeObject(fragment,anchor,selectedObject);
                }

        });
        fragment.getArSceneView().getScene().setOnTouchListener(new Scene.OnTouchListener() {
//NOT WORKING
            @Override
            public boolean onSceneTouch(HitTestResult hitTestResult, MotionEvent motionEvent) {
                Log.d("ON TOUCH LISTENER","FIRED");
                if(hitTestResult.getNode()!=null){
                    Log.d("ON TOUCH LISTENER","FOUND NODE");
                    hitTestResult.getNode().setRenderable(null);
                    hitTestResult.getNode().setParent(null);
                    return true;
                }
               return false;
            }
        });

    }
    private void initializeGallery(ArFragment fragment){
        LinearLayout gallery = findViewById(R.id.gallery_layout);
        ImageView chair = new ImageView(this);
        chair.setImageResource(R.drawable.chair_thumb);
        chair.setContentDescription("chair");
        chair.setOnClickListener(view->{selectedObject=Uri.parse("chair.sfb");});
        gallery.addView(chair);

        ImageView couch = new ImageView(this);
        couch.setImageResource(R.drawable.couch_thumb);
        couch.setContentDescription("couch");
        couch.setOnClickListener(view->{selectedObject=Uri.parse("couch.sfb");});
        gallery.addView(couch);

        ImageView lamp = new ImageView(this);
        lamp.setImageResource(R.drawable.lamp_thumb);
        lamp.setContentDescription("lamp");
        lamp.setOnClickListener(view->{selectedObject=Uri.parse("lamp.sfb");});
        gallery.addView(lamp);

        ImageView trash=new ImageView(this);
        trash.setImageResource(R.drawable.trash_can);
        trash.setMaxHeight(10);
        trash.setMaxWidth(10);
        trash.setOnClickListener(view->{
            List<Node> children = new ArrayList<>(fragment.getArSceneView().getScene().getChildren());
            for(Node n:children){
                if(n instanceof AnchorNode){
                    if (((AnchorNode) n).getAnchor() != null) {
                        ((AnchorNode) n).getAnchor().detach();
                    }
                }
                if (!(n instanceof com.google.ar.sceneform.Camera) && !(n instanceof Sun)) {
                    n.setParent(null);
                }
            }
        });
        gallery.addView(trash);
    }

    private void placeObject(ArFragment fragment, Anchor anchor,Uri model){
        ModelRenderable.builder().setSource(fragment.getContext(),model).build()
                .thenAccept(renderable ->addNodeToScene(fragment,anchor,renderable)).exceptionally((throwable -> {
                    AlertDialog.Builder builder=new AlertDialog.Builder(this);
                    builder.setMessage(throwable.getMessage()).setTitle("Error");
                    AlertDialog dialof=builder.create();
                    dialof.show();
                    return null;
        }));
    }
    private void addNodeToScene(ArFragment fragnment, Anchor anchor, Renderable renderable){
        AnchorNode anchorNode=new AnchorNode(anchor);//Cannot be moved
        TransformableNode node=new TransformableNode(fragnment.getTransformationSystem());//can be scaled, rotated, moved
        node.setRenderable(renderable);
        node.setParent(anchorNode);
        node.setOnTouchListener((hitTestResult, motionEvent) -> {
            Log.d("NODE DETECTED", "NODE DETECTED");
            String data="COORDINATES:\n"+"X: "+anchorNode.getAnchor().getPose().getTranslation()[0]+"\nY: "+anchorNode.getAnchor().getPose().getTranslation()[1]+"\nZ: "+anchorNode.getAnchor().getPose().getTranslation()[2];
            makeToast(data,anchorNode);

            return false;
        });
        fragnment.getArSceneView().getScene().addChild(anchorNode);//added to scene
        node.select();//selected the transformable node for object to be interactive
    }

    private void makeToast(String data,AnchorNode anchor){
        AlertDialog.Builder builder=new AlertDialog.Builder(this);
        builder.setTitle("Node Info");
        builder.setMessage(data);
        builder.setPositiveButton("ok", (dialogInterface, i) -> dialogInterface.dismiss());
        builder.setNegativeButton("Delete Node",(dialogInterface, i) ->{
            anchor.setParent(null);
            anchor.getAnchor().detach();
            dialogInterface.dismiss();
        } );
        AlertDialog dialog=builder.create();
        dialog.show();
    }
}
