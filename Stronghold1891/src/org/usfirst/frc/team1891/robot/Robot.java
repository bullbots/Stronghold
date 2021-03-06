
package org.usfirst.frc.team1891.robot;

import edu.wpi.first.wpilibj.CANTalon;
import edu.wpi.first.wpilibj.Compressor;
import edu.wpi.first.wpilibj.DigitalOutput;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.IterativeRobot;
import edu.wpi.first.wpilibj.Joystick;
import edu.wpi.first.wpilibj.PWM;
import edu.wpi.first.wpilibj.Relay;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;

import java.util.LinkedList;
import org.usfirst.frc.team1891.arduino.Arduino;
import org.usfirst.frc.team1891.drivesystem.*;
import org.usfirst.frc.team1891.drivesystem.DriveSystem.driveModes;
import org.usfirst.frc.team1891.joysticks.*;
import org.usfirst.frc.team1891.machinestate2016.FieldConfig;
import org.usfirst.frc.team1891.machinestate2016.InvalidStateException;
import org.usfirst.frc.team1891.machinestate2016.MachineState;
import org.usfirst.frc.team1891.motorcontroller.TalonSRX;
import org.usfirst.frc.team1891.pneumatics.PnController;
import org.usfirst.frc.team1891.pneumatics.SolenoidBoth;
import org.usfirst.frc.team1891.visionsystem.RobotCentering;


/**
 * The VM is configured to automatically run this class, and to call the
 * functions corresponding to each mode, as described in the IterativeRobot
 * documentation. If you change the name of this class or the package after
 * creating this project, you must also update the manifest file in the resource
 * directory.
 */
public class Robot extends IterativeRobot {


	RobotCentering centerRobo;//This is the class that will control all robot centering code.
	JoystickControl driverJoy;//, coDriverJoy;//The driver joystick.
	JoystickControl coDriverJoy;
	DriveSystem drive;//The driver control system, all driving will be done through this class.
	LinkedList<MotorAndSide> motors;//The linked list that the drive system requres for all motors
	TalonSRX ballCollect;//The ball collector SRX, this is the talon that is in the front of the robot.
	TalonSRX rearShooter;//The shooter ball, this is the shooter talon that is in the rear of the robot.
	TalonSRX elbow;//The is the talon that controls the arm movement.
	PnController pn;//This is the pneumatic control class, it will control all pneumatic movement.
	LinkedList<SolenoidBoth> solenoids;//This list is for all of the solenoids that are on the robot, it will be passed to the PnController, solenoids will be acessed by index.
	LinkedList<Compressor> compressor;//This is a list of all the compressor that are on the robot, it will be passed to the PnController.
	MachineState autoMachine;//This is the class that controls all autonomous movement in auto mode and controls the state machine.
	int autoState=0;//This value represents the state of the machine in auto mode, please refer to the machine state documentation.
	boolean autoStateThreeQuickStop=false;//This value is used in autoPeriodic to provide a boolean flag for a quick stop in state 3.
	boolean autoStateFourQuickStop=false;//The value is used in autoPeriodic to provate a boolean flag for a quick stop in state 4.
	Timer ballShooterRectractTime;//This value is used to control the shooting solenoid so that it does not immediatly retract.
	Arduino lights;//This is the class that controls the arduino lights.
	boolean printedState1=false, printedState2=false, printedState3 =false, printedState4 =false, printedState5 =false, printedState6=false, printedState7=false;//Boolean flags for auto print statements.
	FieldConfig preGameConfig;//Class that controls pregame configuration.
	Relay winch;//Used to extend and retract the wench in teleop mode
	boolean[] testLog;//Just saves test data, used in test periodic
	boolean[] testComplete;//Tracks test progress, used in test periodic.
	String[] componentList;//List of the test numbers, used in test periodic
	Timer armLiftTime;//Used to calibrate timeing for lifting
	boolean elbowWinchTimer=false;//Keeps armLiftTime from being called twice
	double elbowStartingPos;//Used to keep constant values for the arm position
	double elbowDelta = -1845;//The range of elbow values
	/**
	 * Boolean to tell if the ball has been fired or not.
	 */
	public static boolean autoBallFired=false;
	/**
	 * This function is run when the robot is first started up and should be
	 * used for any initialization code.
	 */
	public void robotInit() {
		driverJoy = new JoystickControl();//Joystick for the driver
		driverJoy.init(new Joystick(0), new Joystick(1));//Instantiate joystick for the driver, make sure the co-driver controller is not on port 0 or 1.
		coDriverJoy = new JoystickControl();
		coDriverJoy.init(new Joystick(2), new Joystick(3));
		motors = new LinkedList<MotorAndSide>();//The list of motors to be passed to the drive system
		try {
			motors.add(new MotorAndSide(new TalonSRX(new CANTalon(4)), "RIGHT", true));//Has encoder
			motors.add(new MotorAndSide(new TalonSRX(new CANTalon(3)), "RIGHT", false));
			motors.add(new MotorAndSide(new TalonSRX(new CANTalon(1)), "LEFT",true));//Has encoder
			motors.add(new MotorAndSide(new TalonSRX(new CANTalon(2)), "LEFT", true));
		} catch (InvalidSideException e) {
			e.printStackTrace();
		}
		ballCollect = new TalonSRX(new CANTalon(5));//Instantite shooting talon
		rearShooter = new TalonSRX(new CANTalon(6));//Instantiate shooting talon
		drive= new DriveSystem(motors);//The drive system
		solenoids = new LinkedList<SolenoidBoth>();//List of all solenoids on the robot.
		solenoids.add(new SolenoidBoth(1, 0));//Shooter solenoid
		solenoids.add(new SolenoidBoth(3, 2));//Solenoid that launches ball.
		solenoids.add(new SolenoidBoth(7, 5));//bottom arm solenoid
		solenoids.add(new SolenoidBoth(6, 4));//top arm solenoid
		compressor  = new LinkedList<Compressor>();//List of all compressors (just one)
		compressor.add(new Compressor(0));
		pn = new PnController(solenoids, compressor);//Instantiate the PnController
		pn.turnAllCompressorsOn();//Turn on all compressors.
		centerRobo = new RobotCentering(160, 320);//Instantiate the auto robot centering, for a 320x160 image.
		lights = new Arduino(4);//Instatiate the arduino controller
		elbow = new TalonSRX(new CANTalon(7));//Instantiate the motor controller for the elbow.
		elbow.initVoltage();//Initate the arm motor controller in voltage mode.
		winch = new Relay(0);//Port that the wench is on
		ballShooterRectractTime = new Timer();//Timer for the piston that shoots the ball to retract
		preGameConfig= new FieldConfig();//The configuration that takes place before auto
		armLiftTime = new Timer();//Used when climbing in teleop
	}


	/**
	 * This autonomous (along with the chooser code above) shows how to select between different autonomous modes
	 * using the dashboard. The sendable chooser code works with the Java SmartDashboard. If you prefer the LabVIEW
	 * Dashboard, remove all of the chooser code and uncomment the getString line to get the auto name from the text box
	 * below the Gyro
	 *
	 * You can add additional auto modes by adding additional comparisons to the switch structure below with additional strings.
	 * If using the SendableChooser make sure to add them to the chooser code above as well.
	 */
	public void autonomousInit() {

		//		stateMachine.initiateMachine();
		//		stateMachine.findShotestPath();

		drive.setDriveSystem(driveModes.TANK_DRIVE_PID);
		drive.enableAll();
		/**
		 * All boolean values are set to false at the beginning of auto.
		 */
		printedState1=false;
		printedState2=false;
		printedState3 =false;
		printedState4 =false;
		printedState5 =false; 
		printedState6=false;
		printedState7=false;
		autoMachine = new MachineState();
		autoMachine.initiateMachine();
		preGameConfig.configureRobotStartPosition();
	}
	/**
	 * This function
	 *  is called periodically during autonomous
	 */
	public void autonomousPeriodic() {	

		elbowStartingPos = elbow.getFeedbackValuePosition();
		if (elbow.getFeedbackValuePosition() > elbowStartingPos + 0.3*elbowDelta)
		{
			elbow.setVoltage(-5);
		}
		else if (elbow.getFeedbackValuePosition() < elbowStartingPos + 0.3*elbowDelta && elbow.getFeedbackValuePosition() > elbowStartingPos + 0.4*elbowDelta)
		{
			elbow.setVoltage(-3.3);
		}
		else
		{
			elbow.setVoltage(2);
		}
//		autoMachine.update();
//		try {
//			autoState=autoMachine.getState();
//		} catch (InvalidStateException e) {
//			e.printStackTrace();
//
//		}
//
//		switch(autoState){
//		case 0://Finding crossable defense
//			System.out.println("State 0");
//			break;
//		case 1:// Routing to defense
//			if(!printedState1){
//				System.out.println("State 1");
//				printedState1=true;
//			}
//			autoMachine.findShotestPath();
//
//			break;
//		case 2://Moving to defense
//			if(!printedState2){
//				System.out.println("State 2");
//				printedState2=true;
//			}
//			drive.drive(new JoyVector(-0.15,-1,0,0));//Go forwards
//			break;
//		case 3://Crossing defense
//			if(!printedState3){
//				System.out.println("State 3");
//				printedState3=true;
//			}
//			if(autoMachine.isTurnLeft()){
//				drive.drive(new JoyVector(-0.3,0,0,0));
//			}else if(autoMachine.isTurnRight()){
//				drive.drive(new JoyVector(0.3,0,0,0));
//			}else{
//				if(!autoStateThreeQuickStop){//Do a quick stop to stop the turning.
//					drive.drive(new JoyVector(0,0,0,0));
//					Timer.delay(0.1);
//					autoStateThreeQuickStop=true;
//				}
//				drive.drive(new JoyVector(-0.15,-1,0,0));
//			}
//			break;
//		case 4://Moving to shooting postion
//			if(!printedState4){
//				System.out.println("State 4");
//				printedState4=true;
//			}
//			drive.drive(new JoyVector(0,0,0,0));
//			if(autoMachine.isTurnLeft()){
//				drive.drive(new JoyVector(-0.3,0,0,0));
//			}else if(autoMachine.isTurnRight()){
//				drive.drive(new JoyVector(0.3,0,0,0));
//			}else{
//				if(!autoStateFourQuickStop){//Do a quick stop to stop the turning.
//					drive.drive(new JoyVector(0,0,0,0));
//					Timer.delay(0.1);
//					autoStateFourQuickStop=true;
//				}
//				drive.drive(new JoyVector(-0.15,-1,0,0));
//			}
//			break;
//
//		case 5://Finding goal target
//			if(!printedState5){
//				System.out.println("State 5");
//				printedState5=true;
//			}
//			JoyVector tmp = centerRobo.centerRobot();
//			if(tmp!=null){
//				drive.drive(tmp);
//			}
//			break;
//		case 6://Firing at target
//			if(!printedState6){
//				System.out.println("State 6");
//				printedState6=true;
//			}
//			//Firing code
//			break;
//		case 7://End autonomous 
//			if(!printedState7){
//				System.out.println("State 7");
//				printedState7=true;
//			}
//			//#Dance
//			break;
//		}

	}


	public void teleopInit(){
		System.out.println("In teleop init");
		drive.setDriveSystem(driveModes.TANK_DRIVE);
		drive.enableAll();
		autoMachine = new MachineState();

		if (DriverStation.getInstance().getAlliance() == DriverStation.Alliance.Blue){
			lights.writeTeam(0);
		}
		else{
			lights.writeTeam(1);
		}
	}



	/**
	 * 
	 * This function is called periodically during operator control
	 */
	public void teleopPeriodic() {
		drive.drive(driverJoy.getData());
		
		this.elbowControl();
		this.ballCollectionAndShooting();
		
		//smartDashboard data - may or may not work
		SmartDashboard.putBoolean("Axe Raised", coDriverJoy.buttonToggle(2));
		SmartDashboard.putBoolean("EndGame Started", coDriverJoy.buttonToggle(6));
		SmartDashboard.putBoolean("Manual Mode", coDriverJoy.buttonToggle(10));
		SmartDashboard.putBoolean("extending", coDriverJoy.buttonToggle(7));
		SmartDashboard.putBoolean("Retracting", coDriverJoy.buttonToggle(8));
	}


	public void testInit(){
		drive.setDriveSystem(driveModes.TANK_DRIVE_PID);
		drive.enableAll();
		testComplete = new boolean[11];
		for (int x=0; x<testComplete.length; x++)
		{
			testComplete[x] = false;
		}
		testLog = new boolean[10];
		componentList[0] = "drive system";
		componentList[1] = "shooting wheel";
		componentList[2] = "collecting wheel";
		componentList[3] = "elbow";
		componentList[4] = "trigger solinoid";
		componentList[5] = "trigger solinoid";
		componentList[6] = "shooter lift solinoid extend";
		componentList[7] = "shooter lift solinoid retract";
		componentList[8] = "bottom arm solinoid extend";
		componentList[9] = "bottom arm solinoid retract";
		componentList[10] = "top arm solinoid extend";
		componentList[11] = "top arm solinoid retract";
		componentList[12] = "winch retract";
		componentList[13] = "whinch extend";
		System.out.println(componentList[0]);
		System.out.println("press 1 for success, 2 for fail");
	}

	/**
	 * This function is called periodically during test mode
	 */
	public void testPeriodic() {
		drive.drive(driverJoy.getData());	
		// drive test - robot will drive
		if (!testComplete[0])
		{
			drive.drive(driverJoy.getData());
			if (driverJoy.buttonControlled(1))
			{
				testLog[0] = true;
				drive.drive(new JoyVector(0,0,0,0));
				testComplete[0] = true;
				System.out.println(componentList[1]);
				System.out.println("press 1 for success, 2 for fail");
			}
			else if (driverJoy.buttonControlled(2))
			{
				testLog[0] = false;
				drive.drive(new JoyVector(0,0,0,0));
				testComplete[0] = true;
				System.out.println(componentList[1]);
				System.out.println("press 1 for success, 2 for fail");
			}
		}
		// back shooting wheel
		else if (!testComplete[1])
		{
			rearShooter.setVoltage(4);
			if (driverJoy.buttonControlled(1))
			{
				testLog[1] = true;
				rearShooter.setVoltage(0);
				testComplete[1] = true;
				System.out.println(componentList[2]);
				System.out.println("press 1 for success, 2 for fail");
			}
			else if (driverJoy.buttonControlled(2))
			{
				testLog[1] = false;
				rearShooter.setVoltage(0);
				testComplete[1] = true;
				System.out.println(componentList[2]);
				System.out.println("press 1 for success, 2 for fail");
			}
		}
		//collecting and shooting wheel
		else if (!testComplete[2])
		{
			ballCollect.setVoltage(4);
			if (driverJoy.buttonControlled(1))
			{
				testLog[2] = true;
				ballCollect.setVoltage(0);
				testComplete[2] = true;
				System.out.println(componentList[3]);
				System.out.println("press 1 for success, 2 for fail");
			}
			else if (driverJoy.buttonControlled(2))
			{
				testLog[2] = false;
				ballCollect.setVoltage(0);
				testComplete[2] = true;
				System.out.println(componentList[3]);
				System.out.println("press 1 for success, 2 for fail");
			}
		}
		//the arm elbow motor
		else if (!testComplete[3])
		{
			elbow.setVoltage(-3);
			if (driverJoy.buttonControlled(1))
			{
				testLog[3] = true;
				elbow.setVoltage(0);
				testComplete[3] = true;
				System.out.println(componentList[4]);
				System.out.println("press 1 for success, 2 for fail");
			}
			else if (driverJoy.buttonControlled(2))
			{
				testLog[3] = false;
				elbow.setVoltage(0);
				testComplete[3] = true;
				System.out.println(componentList[4]);
				System.out.println("press 1 for success, 2 for fail");
			}
		}
		//ball pushing solenoid
		else if (!testComplete[4])
		{
			pn.extend(1);
			if (driverJoy.buttonControlled(1))
			{
				testLog[4] = true;
				testComplete[4] = true;
				System.out.println(componentList[5]);
				System.out.println("press 1 for success, 2 for fail");
			}
			else if (driverJoy.buttonControlled(2))
			{
				testLog[4] = false;
				testComplete[4] = true;
				System.out.println(componentList[5]);
				System.out.println("press 1 for success, 2 for fail");
			}
		}
		else if (!testComplete[5])
		{
			pn.retract(1);
			if (driverJoy.buttonControlled(1))
			{
				testLog[5] = true;
				testComplete[5] = true;
				System.out.println(componentList[6]);
				System.out.println("press 1 for success, 2 for fail");
			}
			else if (driverJoy.buttonControlled(2))
			{
				testLog[5] = false;
				testComplete[5] = true;
				System.out.println(componentList[6]);
				System.out.println("press 1 for success, 2 for fail");
			}
		}
		//shooter solenoid
		else if (!testComplete[6])
		{
			pn.extend(0);
			if (driverJoy.buttonControlled(1))
			{
				testLog[6] = true;
				testComplete[6] = true;
				System.out.println(componentList[7]);
				System.out.println("press 1 for success, 2 for fail");
			}
			else if (driverJoy.buttonControlled(2))
			{
				testLog[6] = false;
				testComplete[6] = true;
				System.out.println(componentList[7]);
				System.out.println("press 1 for success, 2 for fail");
			}
		}
		else if (!testComplete[7])
		{
			pn.retract(0);
			if (driverJoy.buttonControlled(1))
			{
				testLog[7] = true;
				testComplete[7] = true;
				System.out.println(componentList[8]);
				System.out.println("press 1 for success, 2 for fail");
			}
			else if (driverJoy.buttonControlled(2))
			{
				testLog[7] = false;
				testComplete[7] = true;
				System.out.println(componentList[8]);
				System.out.println("press 1 for success, 2 for fail");
			}
		}
		//bottom arm solenoids
		else if (!testComplete[8])
		{
			pn.extend(2);
			if (driverJoy.buttonControlled(1))
			{
				testLog[8] = true;
				testComplete[8] = true;
				System.out.println(componentList[9]);
				System.out.println("press 1 for success, 2 for fail");
			}
			else if (driverJoy.buttonControlled(2))
			{
				testLog[8] = false;
				testComplete[8] = true;
				System.out.println(componentList[9]);
				System.out.println("press 1 for success, 2 for fail");
			}
		}
		else if (!testComplete[9])
		{
			pn.retract(2);
			if (driverJoy.buttonControlled(1))
			{
				testLog[9] = true;
				testComplete[9] = true;
				System.out.println(componentList[10]);
				System.out.println("press 1 for success, 2 for fail");
			}
			else if (driverJoy.buttonControlled(2))
			{
				testLog[9] = false;
				testComplete[9] = true;
				System.out.println(componentList[10]);
				System.out.println("press 1 for success, 2 for fail");
			}
		}
		//top arm solenoids
		else if (!testComplete[10])
		{
			pn.extend(3);
			if (driverJoy.buttonControlled(1))
			{
				testLog[10] = true;
				testComplete[10] = true;
				System.out.println(componentList[11]);
				System.out.println("press 1 for success, 2 for fail");
			}
			else if (driverJoy.buttonControlled(2))
			{
				testLog[10] = false;
				testComplete[10] = true;
				System.out.println(componentList[11]);
				System.out.println("press 1 for success, 2 for fail");
			}
		}
		else if (!testComplete[11])
		{
			pn.retract(3);
			if (driverJoy.buttonControlled(1))
			{
				testLog[11] = true;
				testComplete[11] = true;
				System.out.println(componentList[12]);
				System.out.println("press 1 for success, 2 for fail");
			}
			else if (driverJoy.buttonControlled(2))
			{
				testLog[11] = false;
				testComplete[11] = true;
				System.out.println(componentList[12]);
				System.out.println("press 1 for success, 2 for fail");
			}
		}
		//winch retracts then extends
		else if (!testComplete[12])
		{
//			winch.setRaw(true);
			if (driverJoy.buttonControlled(1))
			{
				testLog[12] = true;
				testComplete[12] = true;
				System.out.println(componentList[13]);
				System.out.println("press 1 for success, 2 for fail");
			}
			else if (driverJoy.buttonControlled(2))
			{
				testLog[12] = false;
				testComplete[12] = true;
				System.out.println(componentList[13]);
				System.out.println("press 1 for success, 2 for fail");
			}
		}
		else if (!testComplete[12])
		{
//			winch.setRaw(false);
			if (driverJoy.buttonControlled(1))
			{
				testLog[12] = true;
				testComplete[12] = true;
				System.out.println(componentList[13]);
				System.out.println("press 1 for success, 2 for fail");
			}
			else if (driverJoy.buttonControlled(2))
			{
				testLog[12] = false;
				testComplete[12] = true;
				System.out.println(componentList[13]);
				System.out.println("press 1 for success, 2 for fail");
			}
		}
		else if (!testComplete[13])
		{
			for (int x=0; x<12; x++)
			{
				System.out.println(componentList[x] + " - " +testLog[x]);
			}
			testComplete[13] = true;
		}
	}

	/**
	 * Code that controls the elbow
	 */
	public void elbowControl(){
		winch.setDirection(Relay.Direction.kForward);
		if (coDriverJoy.buttonToggle(6))//This is for endgame lifting mode
		{
			lights.writeState(4);
			if (!coDriverJoy.buttonToggle(7))//Endgame rasing and extending
			{
				if (!coDriverJoy.buttonToggle(10))
				{
					if (elbow.getFeedbackValuePosition() > elbowStartingPos + 0.75*elbowDelta)
					{
						elbow.setVoltage(-6);
					}
					else if (elbow.getFeedbackValuePosition() < elbowStartingPos + 0.75*elbowDelta && elbow.getFeedbackValuePosition() > elbowStartingPos + 0.9*elbowDelta)
					{
						elbow.setVoltage(0);
					}
					else if (elbow.getRawVoltageOutput() < elbowStartingPos + 0.9*elbowDelta)
					{
						elbow.setVoltage(1);
					}
				}
				else
				{
					if (coDriverJoy.getData().getY_comp() < 0)
					{
						elbow.setVoltage(-4*coDriverJoy.joy1Axis(1));
					}
					else if (coDriverJoy.getData().getY_comp() > 0)
					{
						elbow.setVoltage(-8*coDriverJoy.getData().getY_comp());
					}
					else
					{
						elbow.setVoltage(0);
					}
				}
			}
			else
			{
				
				if (coDriverJoy.buttonToggle(8))
				{
					pn.retract(2);
					pn.retract(3);
					if(!elbowWinchTimer){
						armLiftTime.start();
						elbowWinchTimer=true;
					}
					if (armLiftTime.get() > 0.5)
					{
						winch.set(Relay.Value.kOn);
						armLiftTime.stop();
						armLiftTime.reset();
						lights.writeState(6);
					}
				}else{
					pn.extend(2);
					pn.extend(3);
					elbow.setVoltage(-3);
				}
			}
		}
		else if (coDriverJoy.buttonToggle(2))
		{
			
			if (elbow.getFeedbackValuePosition() > elbowStartingPos + 0.3*elbowDelta)
			{
				elbow.setVoltage(-5);
			}
			else if (elbow.getFeedbackValuePosition() < elbowStartingPos + 0.3*elbowDelta && elbow.getFeedbackValuePosition() > elbowStartingPos + 0.4*elbowDelta)
			{
				elbow.setVoltage(-3.3);
			}
			else
			{
				elbow.setVoltage(2);
			}
		}
		else
		{
			if (coDriverJoy.getData().getY_comp() < 0)
			{
				elbow.setVoltage(-4*coDriverJoy.joy1Axis(1));
			}
			else if (coDriverJoy.getData().getY_comp() > 0)
			{
				elbow.setVoltage(-8*coDriverJoy.getData().getY_comp());
			}
			else
			{
				elbow.setVoltage(0);
			}
		}
		if (coDriverJoy.button(11))
		{
			elbowStartingPos = elbow.getFeedbackValuePosition();
		}
		System.out.println("Min: "+elbowStartingPos);
		System.out.println("Max: "+elbow.getFeedbackValuePosition());
		System.out.println("Delta: "+(elbow.getFeedbackValuePosition()-elbowStartingPos));
	}

	/**
	 * Code that controls all the shooter code.
	 */
	public void ballCollectionAndShooting(){
		if(coDriverJoy.button(4)){//Moves shooter into ball collecting position
			ballCollect.setVoltage(5);
			rearShooter.setVoltage(5);
			pn.extend(0);
		}else{//Do nothing
			pn.retract(0);
			ballCollect.setVoltage(0);
			rearShooter.setVoltage(0);
		}

		if(driverJoy.button(1)){//Extends the piston to shoot the ball, else it will retract after a second of extension
			pn.extend(1);
			ballShooterRectractTime.start();
		}else if(ballShooterRectractTime.get()>1){
			pn.retract(1);
			ballShooterRectractTime.stop();
			ballShooterRectractTime.reset();
		}

		if(coDriverJoy.button(3)){//Begins spinner the shooting wheels in the correct direction to shoot
			ballCollect.setVoltage(5);
			rearShooter.setVoltage(-6);
		}
	}
}
