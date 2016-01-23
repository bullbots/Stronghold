package org.usfirst.frc.team1891.joysticks;

/**
 * @author Tyler
 *This class is used to pass joystick data back and forth in a consistant and formatted manor
 */
public class JoyVector {
	private double x_comp;
	private double y_comp;
	private double z_comp;
	private double angle;
	/**
	 * This constructor instantiates all data and is built with get methods.
	 * @param x_comp formatted x data from the joystick.
	 * @param y__comp formatted y data from the joystick.
	 * @param z_comp formatted z data from the joystick.
	 * @param angle formatted angle data from the joystick.
	 */
	public JoyVector(double x_comp, double y__comp, double z_comp, double angle){
		
	}
	/**
	 * @return  the x componant of the vector
	 */
	public double getX_comp() {
		return x_comp;
	}
	/**
	 * @return  the y componant of the vector
	 */
	public double getY_comp() {
		return y_comp;
	}
	/**
	 * @return  the z componant of the vector
	 */
	public double getZ_comp() {
		return z_comp;
	}
	/**
	 * @return  the calculated angle.
	 */
	public double getAngle() {
		return angle;
	}
}
