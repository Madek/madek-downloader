# Madek Downloader

## Usage

### General

This is a java application which is invoked from the terminal/console
like the following:

    java -jar madek-downloader-VERSION-standalone.jar

We will not repeat this for the following commands.

The downloader requires Java 1.7 or later, check the available version with:

    $ java -version
    java version "1.8.0_60"
    Java(TM) SE Runtime Environment (build 1.8.0_60-b27)
    Java HotSpot(TM) 64-Bit Server VM (build 25.60-b23, mixed mode)

### Help

Add the argument `--help` or `-h`.

### Provide and Check Credentials

There are two ways to authenticate: either by providing a session-token or with
a name and password pair.


#### Session Token

Providing a session-token is the preferred method for user accounts. It is more
secure and it also works with external authentication providers.

To obtain a session-token open your Madek instance in a web browser. Sign in
and visit `/my/sesstion-token`. Copy the displayed string, this is your
session-token.

**The session token will expire after 24 hours.**

To check the credentials invoke with the following command and arguments:

```
check-credentials --session-token 'GVuALuN_Kev33x973PAjyQ~eQ_i-CYEECA5l0ukYUw4Pc4Z7IV5q4abC6LfvD3blCl417GpHWOvyo6XyoKXCPAwe0N1pn1HqDkAB2vMBL-My0rylIiBCPlwDHPEj0ZEKCDvBPIgiuRnH7obyC7o3ejvaAeNNoLBiqRwZSctcYcI0jiNt_oMU61bTW-Uua6oO__szgW3MR347Kzzheh5v6UYn4sCUSyhL6ZbqHTnATeSRmFgvaXOyJvfxIwnMxHlRpw'
[INFO  Dez-22-Di_14:04:57 madek.downloader.main main] {:options {:login nil, :password nil, :url http://test.madek.zhdk.ch/api, :session-token GVuALuN_Kev33x973PAjyQ~eQ_i-CYEECA5l0ukYUw4Pc4Z7IV5q4abC6LfvD3blCl417GpHWOvyo6XyoKXCPAwe0N1pn1HqDkAB2vMBL-My0rylIiBCPlwDHPEj0ZEKCDvBPIgiuRnH7obyC7o3ejvaAeNNoLBiqRwZSctcYcI0jiNt_oMU61bTW-Uua6oO__szgW3MR347Kzzheh5v6UYn4sCUSyhL6ZbqHTnATeSRmFgvaXOyJvfxIwnMxHlRpw, :target-dir /Users/thomas/Programming/MADEK/miz-downloader}}
[INFO  Dez-22-Di_14:04:58 madek.downloader.core main] {:authentication-method Session, :created_at 2012-02-20T11:14:33.000Z, :id 653bf621-45c8-4a23-a15e-b29036aa9b10, :login tschank, :type User}
```

#### Name and Password

API-Clients must use their combination of name and password.

### Download

To download the contents of a set invoke the `download` command

    download {ID}

The `{ID}` is the UUID of the set. If no credentials are provided the
downloader will download public downloadable media-entries only!

## Build

  lein uberjar


## License

Copyright © 2015 Zurich University of the Arts

The "Madek Downloader" is Free Software released to the public under the GNU General Public License (GPL) v3.
