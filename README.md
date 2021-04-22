# Keyple Android Control demo app

This is the repository for the 'Eclipse Keyple' Android demo control application.

It implements simultaneously multiple plugins using the flavors feature for handling mutiple devices :

- [Coppernic](https://github.com/calypsonet/keyple-android-plugin-coppernic)
- [Famoco](https://github.com/calypsonet/keyple-famoco)
- [NFC Reader / OMAPI](https://github.com/calypsonet/keyple-java/tree/develop/android/keyple-plugin)

Here is the link to the github repository containing the source code :

[calypsonet/keyple-android-demo-control](https://github.com/calypsonet/keyple-android-demo-control)


## Screens
- Settings (SettingsActivity) : Allows to set the settings of the control procedure :
  - Location : Where the control is taking place. If the validation occurred in a different location, the controlled contract will not be considered as valid.
  - Validity Duration : Period (in minutes) during which the contract is considered valid.
- Home (HomeActivity) : Allows to launch the card hunting phase
- Card Reader (CardReaderActivity) : Launches the flavor associated Keyple plugin. At this point the user must present the card (PO) that he wishes to control.
  - Initiaze the Keyple plugin : start detection on NFC and SAM (if available) readers
  - Prepare and defines the default selection requests to be processed when a card is inserted.
  - Listens to detected card tags
  - launches the Control Procedure when a tag is detected
- Control result screen (CardReaderActivity) : displays the controlled card content informations
  - Contracts list
  - Validations list
- Invalid control screen (NetworkInvalidActivity) : displayed when the control procedure failed

## Calypso applications
This demo evolves to provide wide Calypso applet support. For now this demo can support:

* Calypso Prime sample: AID 315449432E49434131h - Structure 05h (CD Light/GTML)
* (Work in progress) Calypso Light sample: AID 315449432E49434133h - Structure 32h (Light Classic)
* Navigo test card: AID  A0000004040125090101h - Structure D7h (Navigo)

## Dependencies

The Android-Keyple library needs multiple dependencies to work.

First we need to import the keyple related dependencies in the `build.gradle` file :

```groovy
    implementation "org.eclipse.keyple:keyple-java-core:1.0.0"
    implementation "org.eclipse.keyple:keyple-java-calypso:1.0.0"
```

Then each devices needs it own dependencies imported. In our case, we use the flavor feature to import only the currently flavor specific device needed dependency.

Here are some examples :

- Coppernic
```groovy
    copernicImplementation "org.eclipse.keyple:keyple-android-plugin-coppernic-ask:1.0.0"
```

- Famoco
```groovy
    famocoImplementation "org.eclipse.keyple:keyple-android-plugin-nfc:1.0.0"
    famocoImplementation "org.eclipse.keyple:keyple-android-plugin-famoco-se-communication:1.0.0"
```

- NFC Reader / OMAPI
```groovy
    omapiImplementation "org.eclipse.keyple:keyple-android-plugin-nfc:1.0.0"
    omapiImplementation "org.eclipse.keyple:keyple-android-plugin-omapi:1.0.0"
```

## Device specific flavors

In Android, a flavor is used to specify custom features. In our case, the specific feature is the device used to run the demo app
and therefore the specific Keyple plugin associated.
This app implements multiple devices plugin at once using this flavor feature.

This feature allows to add a new plugin easily by creating a new flavor and implementing the following classes :
- ReaderModule : Dagger module class that provides needed components :
  - IReaderRepository : Interface used by the app to communicate with a specific Keyple Android plugin. It implements a set of methods used in the card reader screen to initialize, detect, and communicate with a contactless (card) and contact (SAM) Portable Object.
  - ReaderObservationExceptionHandler : Provides a channel for notifying runtime exceptions that may occur during operations carried out by the monitoring thread(s).
- XXXReaderModule : Class implementing the IReaderModule specific to each device plugin, for example 'CoppernicReaderModule'

In order the make a new flavor work, for example for the Coppernic device, you must declare it in the app's build.gradle file.

Add a product flavor to the `device` flavor dimension
```groovy
    flavorDimensions 'device'
    
    productFlavors {
        copernic {
            dimension 'device'
            resValue "string", "app_name", "Keyple Control Copernic"
            applicationIdSuffix ".copernic"
        }
    }
```

Create the flavors sourceSet folder `copernic` in the `app/src` folder.  
Then create in the `copernic` folder the package folders that will contain the code classes : `org/eclipse/keyple/demo/control/`

Declare the sourceSet folder associated to the flavor int the buid.gradle file :
```groovy
    sourceSets {
        main.java.srcDirs += 'src/main/kotlin'
        test.java.srcDirs += 'src/test/kotlin'
        
        copernic.java.srcDirs += 'src/copernic/kotlin'
    }
```

Import the associated plugin dependencies using the specific implementation syntax.  
This way it will only be imported if the specific flavors in active.
```groovy
    copernicImplementation "org.eclipse.keyple:keyple-android-plugin-coppernic-ask:1.0.0"
```

## Ticketing implementation

As we have seen previously, the first step in implementing the ticketing layer is the implementation of the IReaderRepository interface specific the currently used device.
Here are the other classes that allows to use
- CardReaderApi
- TicketingSession
- PoObserver

### CardReaderApi

Mainly used to manage the lifecycle of the keyple plugin. this class is used to initialize the plugin and manage the hunt phase.
It is called on the different steps of the card reader activity lifecycle :
- onResume:
  - Initialize the plugin (PO and SAM readers...)
  - Get the ticketing session
  - Start NFC detection
- onPause :
  - Stop NFC detection
- onDestroy :
  - Clear the keyple plugin (remove observers and unregister plugin)

### TicketingSession

The purpose of this class is to communicate with the portable object (=PO, ex: card).

First it prepares the default selection that will be sent to the PO when a tag is detected by setting the AID(s) and the reader protocol(s) of the cards we want to detect and read.

Once a tag is detected, the TicketingSession processes the default selection by retrieving the current CalypsoPo object. This CalypsoPo contains informations about the card (SerialNumber, PoRevision...)

Finally this class is responsible for launching the control procedure and returning its result.

### PoObserver

This class is the reader observer and inherits from Keyple's class :
```groovy
    ObservableReader.ReaderObserver
```
It is class each time a new ReaderEvent (CARD_INSERTED, CARD_MATCHED...) is launched by the Keyple plugin.
This reader is registered when the reader is registered and removed when the reader is unregistered.

## Control Procedure

For this control demo application, a simple example control procedure has been implemented. This procedure is implemented in the class 'ControlProcedure'.

Opening a secure session is optional for this procedure since we do not need to write anything on the card. So we check if the SAM is present at the beginning, but we keep on with the procedure if not.

This procedure's main steps :
- Event Analysis :
  - Read the environement :
    - If EnvVersionNumber of the Environment structure is not the expected one (==1 for the current version) reject the card.
    - If EnvEndDate points to a date in the past reject the card.
  - Read the last event record :
    - If EventVersionNumber is not the expected one (==1 for the current version) reject the card (if ==0 return error status indicating clean card).
    - If EventLocation != value configured in the control terminal set the validated contract not valid flag as true and skip to contract analysis.
    - Else If EventDateStamp points to a date in the past set the validated contract not valid flag as true and skip to contract analysis.
    - Else If (EventTimeStamp + Validation period configure in the control terminal) < current time of the control terminal set the validated contract not valid flag as true.
- Contract Analysis : For each contract :
  - Retrieve contract used for last event
  - If the ContractVersionNumber == 0 then the contract is blank, move on to the next contract.
  - If ContractVersionNumber is not the expected one (==1 for the current version) reject the card.
  - If ContractValidityEndDate points to a date in the past mark contract as expired.
  - If EventContractUsed points to the current contract index & not valid flag is false then mark it as Validated.
  - Add contract data to the list of contracts read to return to the upper layer.