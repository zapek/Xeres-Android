# Xeres Android Mobile Client

This is the Android mobile client for [Xeres](https://github.com/zapek/Xeres).

With it you can connect to your running Xeres instance at home or in a data center, easily
and securely.

## Supported features

- Contact list
- Private messages
- Chat rooms
- Secured HTTPS link with certificate pinning

## How to run

- install a [Xeres](https://github.com/zapek/Xeres) instance 0.7.4 or higher
- go to _Settings_ / _Remote_ and enable *Remote Access*
- in the Android mobile client, to to _Settings_ and use the same parameters

## FAQ

**Q: Will more features than chat be supported?**

**A:** Of course.

**Q: I can't connect when I'm outside my home**

**A:** Make sure the connecting port is opened on your router if you're behind a NAT. You can use UPNP
for that as Xeres has direct support for it. Make sure `Set with UPNP` is ticked in the Remote settings.

**Q: I don't have a fixed IP. How can I make sure I reach my remote at all time?**

**A:** Use a dynamic DNS provider at home and enter the dynamic hostname in the Android client.

**Q: Will I get notifications when there's a new message?**

**A:** Not currently. Notifications are typically implemented using Google's C2DM, I mean, GCM, er.. I mean FCM
or whatever they changed it to. Since Xeres is decentralized, it's not possible to use
such a centralized system so the next best thing is to implement some kind of intellingent
polling (which the Android OS doesn't like so it has to be done carefully). There will be something.

**Q: Why isn't Xeres on Google's Play Store?**

**A:** Because Google's requirements for being on the Play Store have become ridiculous, even worse than Apple.
They also require that application developers hand them the private keys to sign the app (which means they can modify it
as they see fit, for one particular user or a particular group of users without anyone ever knowing). This is a
serious drawback which means the primary method of distribution will be a plain APK with an auto update feature.
I might still try to do a special version for the Play Store and other less brain dead stores will be considered, too.
