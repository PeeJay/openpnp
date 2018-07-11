/*
 * Copyright (C) 2011 Jason von Nieda <jason@vonnieda.org>
 * 
 * This file is part of OpenPnP.
 * 
 * OpenPnP is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * OpenPnP is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with OpenPnP. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * For more information about OpenPnP visit http://openpnp.org
 */

package org.openpnp.machine.reference.camera;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamCompositeDriver;
import com.github.sarxos.webcam.WebcamDevice;
import com.github.sarxos.webcam.WebcamImageTransformer;
import com.github.sarxos.webcam.ds.buildin.WebcamDefaultDriver;
import com.github.sarxos.webcam.ds.ipcam.IpCamDevice;
import com.github.sarxos.webcam.ds.ipcam.IpCamDeviceRegistry;
import com.github.sarxos.webcam.ds.ipcam.IpCamDriver;
import com.github.sarxos.webcam.ds.ipcam.IpCamMode;
import com.github.sarxos.webcam.util.jh.JHGrayFilter;
import org.openpnp.CameraListener;
import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.ReferenceCamera;
import org.openpnp.machine.reference.camera.wizards.MjpegIPCameraConfigurationWizard;
import org.openpnp.spi.PropertySheetHolder;
import org.simpleframework.xml.Attribute;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


/**
 * A Camera implementation based on the webcam-capture library
 */
public class MjpegIPCamera extends ReferenceCamera implements Runnable, WebcamImageTransformer {

    @Attribute(required = false)
    protected String deviceId = "###DEVICE###";
    @Attribute (required = false)
    protected String ipCamUrl = "http://127.0.0.1/image_stream";

    protected Webcam ipCam;
    private Thread thread;
    private boolean forceGray;
    private BufferedImage image;

    private static final JHGrayFilter GRAY = new JHGrayFilter();

    //Create composite IP Cam/Internal cam driver
    public static class MyCompositeDriver extends WebcamCompositeDriver {
        public MyCompositeDriver() {
            add(new WebcamDefaultDriver());
            add(new IpCamDriver());
        }
    }

    // register custom composite driver
    static {
        Webcam.setDriver(new MyCompositeDriver());
    }

    @Override
    public BufferedImage transform(BufferedImage image) {
        return GRAY.filter(image, null);
    }

    public MjpegIPCamera() {
        try {
            IpCamDeviceRegistry.register("Pi Cam", "http://192.168.0.220:8080/?action=stream_1", IpCamMode.PUSH);
            IpCamDeviceRegistry.register("USB Cam", "http://192.168.0.220:8080/?action=stream_0", IpCamMode.PUSH);
        }
        catch (Exception e) {
            //Do something here?
        }

    }

    @Override
    public synchronized BufferedImage internalCapture() {
        if (thread == null) {
            setDeviceId(deviceId);
        }
        if (thread == null) {
            return null;
        }
        try {
            BufferedImage img = ipCam.getImage();
            return transformImage(img);
        }
        catch (Exception e) {
            return null;
        }
    }

    @Override
    public synchronized void startContinuousCapture(CameraListener listener, int maximumFps) {
        if (thread == null) {
            setDeviceId(deviceId);
        }
        super.startContinuousCapture(listener, maximumFps);
    }

    private BufferedImage lastImage = null;
    private BufferedImage redImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);


    public void run() {
        while (!Thread.interrupted()) {
            try {
                BufferedImage image = internalCapture();
                if (image == null) {
                    image = redImage;
                }
                broadcastCapture(image);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            try {
                Thread.sleep(1000 / 30);
            }
            catch (InterruptedException e) {
                break;
            }
        }
    }

    public String getDeviceId() {
        return deviceId;
    }

    public synchronized void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
        if (thread != null) {
            thread.interrupt();
            try {
                thread.join();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            thread = null;
            ipCam.close();
        }
        try {
            ipCam = null;
            for (Webcam cam : Webcam.getWebcams()) {
                if (cam.getName().equals(deviceId)) {
                    ipCam = cam;
                }
            }
            if (ipCam == null) {
                return;
            }
            ipCam.open();
            if (forceGray) {
                ipCam.setImageTransformer(this);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            return;
        }
        thread = new Thread(this);
        thread.start();
    }

    public void setForceGray(boolean val) {
        forceGray = val;
    }

    public boolean isForceGray() {
        return forceGray;
    }

    public String getIPCamUrl() {
        return ipCamUrl;
    }

    public void setIPCamUrl(String ipCamUrl) {
        this.ipCamUrl = ipCamUrl;
    }



    @Override
    public Wizard getConfigurationWizard() {
        return new MjpegIPCameraConfigurationWizard(this);
    }

    @Override
    public String getPropertySheetHolderTitle() {
        return getClass().getSimpleName() + " " + getName();
    }

    @Override
    public PropertySheetHolder[] getChildPropertySheetHolders() {
        // TODO Auto-generated method stub
        return null;
    }


    public List<String> getDeviceIds() throws Exception {
        ArrayList<String> deviceIds = new ArrayList<>();
        for (Webcam cam : Webcam.getWebcams()) {
            //Only add IP Cameras to this list
            WebcamDevice dev = cam.getDevice();
            if(dev instanceof IpCamDevice) {
                deviceIds.add(cam.getName());
            }
        }
        return deviceIds;
    }


    @Override
    public void close() throws IOException {
        super.close();
        if (thread != null) {
            thread.interrupt();
            try {
                thread.join();
            }
            catch (Exception e) {

            }
            ipCam.close();
        }
    }
}
