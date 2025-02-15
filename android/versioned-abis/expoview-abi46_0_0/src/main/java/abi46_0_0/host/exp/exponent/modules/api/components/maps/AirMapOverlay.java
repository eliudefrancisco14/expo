package abi46_0_0.host.exp.exponent.modules.api.components.maps;

import android.content.Context;
import android.graphics.Bitmap;

import abi46_0_0.com.facebook.react.bridge.ReadableArray;
import abi46_0_0.com.facebook.react.bridge.ReadableMap;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import java.util.ArrayList;

public class AirMapOverlay extends AirMapFeature implements ImageReadable {

  private GroundOverlayOptions groundOverlayOptions;
  private GroundOverlay groundOverlay;
  private LatLngBounds bounds;
  private float bearing;
  private BitmapDescriptor iconBitmapDescriptor;
  private Bitmap iconBitmap;
  private boolean tappable;
  private float zIndex;
  private float transparency;

  private final ImageReader mImageReader;
  private GoogleMap map;

  public AirMapOverlay(Context context) {
    super(context);
    this.mImageReader = new ImageReader(context, getResources(), this);
  }

  public void setBounds(ReadableArray bounds) {
    LatLng sw = new LatLng(bounds.getArray(0).getDouble(0), bounds.getArray(0).getDouble(1));
    LatLng ne = new LatLng(bounds.getArray(1).getDouble(0), bounds.getArray(1).getDouble(1));
    this.bounds = new LatLngBounds(sw, ne);
    if (this.groundOverlay != null) {
      this.groundOverlay.setPositionFromBounds(this.bounds);
    }
  }

  public void setBearing(float bearing){
    this.bearing = bearing;
    if (this.groundOverlay != null) {
      this.groundOverlay.setBearing(bearing);
    }
  }

  public void setZIndex(float zIndex) {
    this.zIndex = zIndex;
    if (this.groundOverlay != null) {
      this.groundOverlay.setZIndex(zIndex);
    }
  }

  public void setTransparency(float transparency) {
      this.transparency = transparency;
      if (groundOverlay != null) {
          groundOverlay.setTransparency(transparency);
      }
  }

  public void setImage(String uri) {
    this.mImageReader.setImage(uri);
  }

  public void setTappable(boolean tapabble) {
    this.tappable = tapabble;
    if (groundOverlay != null) {
      groundOverlay.setClickable(tappable);
    }
  }


  public GroundOverlayOptions getGroundOverlayOptions() {
    if (this.groundOverlayOptions == null) {
      this.groundOverlayOptions = createGroundOverlayOptions();
    }
    return this.groundOverlayOptions;
  }

  private GroundOverlayOptions createGroundOverlayOptions() {
    if (this.groundOverlayOptions != null) {
      return this.groundOverlayOptions;
    }
    GroundOverlayOptions options = new GroundOverlayOptions();
    if (this.iconBitmapDescriptor != null) {
      options.image(iconBitmapDescriptor);
    } else {
      // add stub image to be able to instantiate the overlay
      // and store a reference to it in MapView
      options.image(BitmapDescriptorFactory.defaultMarker());
      // hide overlay until real image gets added
      options.visible(false);
    }
    options.positionFromBounds(bounds);
    options.zIndex(zIndex);
    options.bearing(bearing);
    options.transparency(transparency);
    return options;
  }

  @Override
  public Object getFeature() {
    return groundOverlay;
  }

  @Override
  public void addToMap(GoogleMap map) {
    GroundOverlayOptions groundOverlayOptions = getGroundOverlayOptions();
    if (groundOverlayOptions != null) {
      this.groundOverlay = map.addGroundOverlay(groundOverlayOptions);
      this.groundOverlay.setClickable(this.tappable);
    } else {
      this.map = map;
    }
  }

  @Override
  public void removeFromMap(GoogleMap map) {
    this.map = null;
    if (this.groundOverlay != null) {
      this.groundOverlay.remove();
      this.groundOverlay = null;
      this.groundOverlayOptions = null;
    }
  }

  @Override
  public void setIconBitmap(Bitmap bitmap) {
    this.iconBitmap = bitmap;
  }

  @Override
  public void setIconBitmapDescriptor(
      BitmapDescriptor iconBitmapDescriptor) {
    this.iconBitmapDescriptor = iconBitmapDescriptor;
  }

  @Override
  public void update() {
    this.groundOverlay = getGroundOverlay();
    if (this.groundOverlay != null) {
      this.groundOverlay.setVisible(true);
      this.groundOverlay.setImage(this.iconBitmapDescriptor);
      this.groundOverlay.setTransparency(this.transparency);
      this.groundOverlay.setClickable(this.tappable);
    }
  }

  private GroundOverlay getGroundOverlay() {
    if (this.groundOverlay != null) {
      return this.groundOverlay;
    }
    if (this.map == null) {
      return null;
    }
    GroundOverlayOptions groundOverlayOptions = getGroundOverlayOptions();
    if (groundOverlayOptions != null) {
      return this.map.addGroundOverlay(groundOverlayOptions);
    }
    return null;
  }
}
