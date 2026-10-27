import * as THREE from "three";

const frustum = new THREE.Frustum(),
  projection = new THREE.Matrix4(),
  sphere = new THREE.Sphere(),
  lastView = new THREE.Matrix4();
let stamp = -1;

/**
 * The camera's view volume for instance culling, rebuilt once per frame. Run culling after the
 * camera has moved for the frame (a useFrame priority between the scene's own updates and the
 * composer's render), so instances never lag a frame behind the view.
 */
export function viewFrustum(camera: THREE.Camera, frame: number) {
  if (frame !== stamp) {
    stamp = frame;
    camera.updateMatrixWorld();
    frustum.setFromProjectionMatrix(
      projection.multiplyMatrices(
        camera.projectionMatrix,
        camera.matrixWorldInverse,
      ),
    );
  }
  return frustum;
}

/** Whether a sphere (with a small margin for the frame's motion) touches the view. */
export function inView(
  view: THREE.Frustum,
  x: number,
  y: number,
  z: number,
  radius: number,
) {
  sphere.center.set(x, y, z);
  sphere.radius = radius * 1.1 + 0.5;
  return view.intersectsSphere(sphere);
}

/** True when the camera moved or turned since the last call with this key. */
export function cameraChanged(camera: THREE.Camera, last: THREE.Matrix4) {
  lastView.multiplyMatrices(camera.projectionMatrix, camera.matrixWorldInverse);
  if (lastView.equals(last)) return false;
  last.copy(lastView);
  return true;
}

/** Priority for culling callbacks: after scene updates (0), before the composer renders (1). */
export const CULL_PRIORITY = 0.5;
