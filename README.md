# OPUS en phone

[Video of it working](https://youtu.be/V81LdTvAP0I)

This project is a unofficial prototype that enables [OPUS en ligne](https://opusenligne.ca/) to be used on the phone, allowing you to reload your OPUS card using only your phone.

Yes, it is possible to use NFC to reload your OPUS card.
No need for a $20 card reader.

## Disclaimer

This is a unofficial application and is not associated with the Société de transport de Montréal.
Be aware that I cannot help you if the application fails while you were purchasing a ticket, eating your money or if your OPUS card gets damaged on use.
Please use at your own risk.

If anyone at the STM does not want this project to be available online, please contact me.

## Usage

There is only a restart button to refresh the page, use with caution as using it while the application is reloading your OPUS, most definitely will eat your money and not reload it properly.

The application will vibrate on detecting your OPUS and will vibrate again when it completes interacting with the card.
Try not to move the card as it might cause issues while it's interacting with it.

## Modules

### Client

The client, located in the `Client` folder, is a simple program that can replace the driver that OPUS en ligne uses.

It can be built and used separately from the Android project.

### Android

The Android project is a simple GUI for accessing the website and hooking up the phone's NFC to the Client for communication with the card.
Disclaimer: I don't make Android applications.

## Notes

### Issue

This is not specific to this application, rather the whole OPUS en ligne application.

For some reason when you reach the stage where the application actually reloads your OPUS, it is possible for the OPUS en ligne to fail to write to your card and not give you back your money.

If the server sends a Close Secure Channel command with invalid numbers, then that means the server knows it didn't successfully write to the card and should cancel the transcation (it does not and eats your money).

I believe this is an problem with how they are processing the commands being sent to the card and if anyone from the STM reads this, please fix this issue.

### Quick random points about the OPUS card

- Uses the [Calypso Standard](https://en.wikipedia.org/wiki/Calypso_(electronic_ticketing_system)) 
- Smartcard complies with ISO/IEC 14443 Type B international standard
  - Hence, can be reloaded using NFC
- You can use any card reader so long as your computer (or Java's smartcardio's library) can recognize it
- Unsure of how long it's been possible to do this (only developed it recently)
- The card stores actual fares
  - Does not have just an ID in it (meaning you can't just replicate it easily)
  - Actually processes commands given to it
    - Readers will all query fares and info from the card on scan

