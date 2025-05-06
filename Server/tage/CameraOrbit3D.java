package tage;

import org.joml.*;

import net.java.games.input.Event;

import java.lang.Math;

import tage.input.InputManager;
import tage.input.action.AbstractInputAction;

public class CameraOrbit3D {
    private Engine engine;
    private Camera camera; // the camera being controleld
    private GameObject avatar; // the target avatar the camera looks at
    private float cameraAzimuth; // rotation around target Y axis
    private float cameraElevation; // elevation of camera above the target
    private float cameraRadius; // distance between camera and target
    private float minimumElevation = 0.0f;
    private float maximumElevation = 89.99f;

    public CameraOrbit3D(Camera cam, GameObject av, String gpName, Engine e) {
        engine = e;
        camera = cam;
        avatar = av;
        cameraAzimuth = 0.0f; // start BEHIND and ABOVE the target
        cameraElevation = 20.0f; // elevation is in degrees
        cameraRadius = 2.0f; // distance from camera to avatar
        setupInputs(gpName);
        updateCameraPosition();
    }

    public void setupInputs(String gp) {
        OrbitAzimuthAction azmAction = new OrbitAzimuthAction();
        OrbitElevationAction elevationAction = new OrbitElevationAction();
        OrbitDistanceAction distanceAction = new OrbitDistanceAction();

        InputManager im = engine.getInputManager();

        // Associate Controller with actions
        im.associateAction(gp,
                net.java.games.input.Component.Identifier.Axis.Z, azmAction,
                InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
        im.associateAction(gp,
                net.java.games.input.Component.Identifier.Axis.RZ, elevationAction,
                InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
        im.associateAction(gp,
                net.java.games.input.Component.Identifier.Button._7, distanceAction,
                InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
        im.associateAction(gp,
                net.java.games.input.Component.Identifier.Button._6, distanceAction,
                InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
   }

    // Compute the camera's azimuth, elevation, and distance, relative to
    // the target in spherical coordinates, then convert to world Cartesian
    // coordinates and set the camera position from that.

    public void updateCameraPosition() {
        Vector3f avatarRot = avatar.getWorldForwardVector();
        double avatarAngle = Math
                .toDegrees((double) avatarRot.angleSigned(new Vector3f(0, 0, -1), new Vector3f(0, 1, 0)));
        float totalAz = cameraAzimuth - (float) avatarAngle;
        double theta = Math.toRadians(totalAz);
        double phi = Math.toRadians(cameraElevation);
        float x = cameraRadius * (float) (Math.cos(phi) * Math.sin(theta));
        float y = cameraRadius * (float) (Math.sin(phi));
        float z = cameraRadius * (float) (Math.cos(phi) * Math.cos(theta));
        camera.setLocation(new Vector3f(x, y, z).add(avatar.getWorldLocation()));
        camera.lookAt(avatar);
    }

    private class OrbitAzimuthAction extends AbstractInputAction {
        public void performAction(float time, Event event) {
            float rotAmount;
            if (event.getValue() < -0.2) {
                rotAmount = -0.35f;
            } else if (event.getValue() > 0.2) {
                rotAmount = 0.35f;
            } else {
                rotAmount = 0.0f;
            }
            cameraAzimuth += rotAmount;
            cameraAzimuth = cameraAzimuth % 360;
            updateCameraPosition();
        }
    }

    private class OrbitElevationAction extends AbstractInputAction {
        public void performAction(float time, Event event) {
            float rotAmount;
            float direction = (event.getComponent().getIdentifier().getName() == "Down") ? 1.0f : -1.0f;
            if (event.getValue() < -0.2) {
                rotAmount = -0.35f;
            } else if (event.getValue() > 0.2) {
                rotAmount = 0.35f;
            } else {
                rotAmount = 0.0f;
            }
            cameraElevation += direction * rotAmount;

            // Camera elevation is the either the minimum elevation, or the maximum
            // elevation if the current camera elevation goes past the threshold
            cameraElevation = Math.max(minimumElevation, Math.min(maximumElevation, cameraElevation));
            updateCameraPosition();
        }
    }

    private class OrbitDistanceAction extends AbstractInputAction {
        public void performAction(float time, Event event) {
            float distanceAmt;
            float direction = (event.getComponent()
                    .getIdentifier() == net.java.games.input.Component.Identifier.Button._7) ? -1.0f : 1.0f;
            if (event.getValue() < -0.2) {
                distanceAmt = -0.2f;
            } else if (event.getValue() > 0.2) {
                distanceAmt = 0.2f;
            } else {
                distanceAmt = 0.0f;
            }
            cameraRadius += direction * distanceAmt * 0.25f; // make zoom less harsh
            cameraRadius = cameraRadius % 360;
            updateCameraPosition();
        }
    }

}
