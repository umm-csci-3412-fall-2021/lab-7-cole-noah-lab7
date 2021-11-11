# Segmented-File-Server-client <!-- omit in toc -->

[![Tests](../../workflows/Bats%20test/badge.svg)](../../actions?query=workflow%3A"Bats+test")

The starter code and (limited) tests for the client code for the Segmented File System lab.

* [Background](#background)
* [Segmenting the files](#segmenting-the-files)
* [The OutOfMoney.com protocol](#the-outofmoneycom-protocol)
  * [The packet structure](#the-packet-structure)
  * [How to construct packet numbers](#how-to-construct-packet-numbers)
* [Writing the client backend](#writing-the-client-backend)
  * [Starting the conversation](#starting-the-conversation)
  * [Processing the packets you receive](#processing-the-packets-you-receive)
* [Testing](#testing)
  * [Unit test your work](#unit-test-your-work)
  * [Check your work by running your client by hand](#check-your-work-by-running-your-client-by-hand)
  * [Check your work using `bats` tests](#check-your-work-using-bats-tests)

## Background

Those wacky folks at OutOfMoney.com are at it again, and have come up with another awesome money making scheme. This time they're setting up a service where users will use a client program to contact a new OutOfMoney.com server (using another old computer they found in the basement at the high school). Every time the client contacts the server, the server will send back three randomly chosen bits of of Justin Bieber arcana. These could be sound files, videos, photos, or text files (things like lyrics). They've got the server up and running, but the kid that had prototyped the client software has moved away suddenly. (His mom works for the CIA and they move a lot, usually with little notice.) Unfortunately he took all the code with him, and isn't responding to any attempts to contact him on Facebook.

In a rare fit of sanity, they've brought you in to help them out by building the backend of the client.

*This is one of the more time-consuming labs, so prepare and plan accordingly.*

This starter code comes with some simple `bats` tests, but as discussed below you'll almost certainly want to add additional JUnit tests of your own to test the design and implementation of your data management tools.

## Segmenting the files

They're using socket-based connections between the client and server, but someone who is long since fired decided that it was important that the server break the files up into 1K chunks and send each chunk separately using UDP. Because of various sources of asynchrony such as threads on the server and network delays, you can't be sure you'll receive the chunks in the right order, so you'll need to collect and re-assemble them before writing out the file.

Unlike the socket connections we used for the Echo Server earlier, this system uses UDP or Datagram sockets. "Regular" sockets (also known as stream sockets or TCP sockets) provide you with a stable two-way connection that remains active until you disconnect. Datagram sockets are less structured, and you essentially just send out packets of information which can arrive at their destination in an arbitrary order, much like the description of packets in Chapter 7 of [Saltzer and Kaashoek](http://ocw.mit.edu/resources/res-6-004-principles-of-computer-system-design-an-introduction-spring-2009/) ([PDF of that chapter](https://ocw.mit.edu/resources/res-6-004-principles-of-computer-system-design-an-introduction-spring-2009/online-textbook/networks_open_5_0.pdf)) or [this online pdf](http://www.ee.columbia.edu/~bbathula/courses/HPCN/chap03_part-1.pdf). On the listening end, instead of reading a stream of data, data arrives in packets, and it's your job to interpret their contents. Typically there is some sort of protocol that describes how packets are formed and interpreted; otherwise we end up with an impossible guessing game.

Java provides direct support for UPD/datagram sockets primarily through the `DatagramSocket` and `DatagramPacket` classes. See the tutorial [Writing a Datagram Client and Server](http://docs.oracle.com/javase/tutorial/networking/datagrams/clientServer.html) for more; what you care about is down in the "The QuoteClient class" section.

:bangbang: ***There are security implications of using UDP,*** which is one of many reasons why one would tend to use TCP instead unless you have a good reason to use UDP. If you establish a datagram socket, you're essentially willing to receive packets from anyone sending to the UDP port you're listening on. In this lab, for example, there's nothing to stop someone from throwing stuff at your UDP port at the same time that the server is sending you files there, and there's no simple way to distinguish legitimate packets from bogus (and possibly) malicious packets. In this case (partly because we're using Java) it's hard to do much more than corrupt the files and (if your error checking isn't great) crash the client. In systems where buffer overruns are possible (typically programmed in C), then attacks on UDP ports can in extreme cases lead to gaining root.

:bangbang: **You probably don't want to use WiFi for this.** Unlike TCP,
UDP doesn't promise that packets will arrive, so it's theoretically possible
that a client won't receive all the packets sent by the server. We don't
typically see that when using the lab boxes as the capacity of the wired
Ethernet network isn't particularly stressed by this. If, however, you bring
in a laptop and run the client there, all the network traffic happens across
the wireless network and there is a much higher probability that packets will
be lost. If this happens, there's no way for the client to "fix" the problem
since the protocol doesn't provide any way to request a re-send for missing
packets.

:bangbang: Make sure you shut down all your client processes before you leave the lab. If you leave them running you can block ports and make it impossible for other people to work on their lab on that computer.

## The OutOfMoney.com protocol

Your job is to write a (Java) program that sends a UDP/datagram packet to the server, and then waits and receives packets from the server until all three files are completely received. When a file is complete, it should be written to disk using the file name sent in the header packet. When all three files have been written to disk, the client should terminate cleanly. As mentioned above, since the file will be broken up into chunks and sent using UDP, we need a protocol to tell us how to interpret the pieces we receive so we can correctly assemble them. Those clever clogs at OutOfMoney.com didn't have much experience (ok, any experience) designing these kinds of protocols, so theirs isn't necessarily the greatest, but it gets the job done.

### The packet structure

In this protocol there are essentially two kinds of packets:

* A header packet with a unique file ID for the file being transferred, and
  the actual name of the file so we'll know what to call it after we've assembled
  the pieces
* A data packet, with the unique file ID (so we know what file this is part of),
  the packet number, and the data for that chunk.

Each packet starts with a status byte that indicates which type of packet it is:

* If the status byte is even (i.e., the least significant bit is 0), then this is a header packet
* If the status byte is odd (i.e., the least significant bit is 1), then this is a data packet
* If the status byte's second bit is also 1 (i.e., it's 3 mod 4), then this is the *last* data packet for this file
  They could have included the number of packets in the header packet, but they chose to to mark the last packet
  instead. Note that the last data packet (in terms of being the last bytes in the file) isn't guaranteed to come
  last, and might come anywhere in the stream including possibly being the *first* packet to arrive.

The packet numbers are consecutive and start from 0. So if a file is split into 18 chunks, there will be 18 data packets numbered 0 through 17, as well as the header packet for that file (for a total of 19 packets). The file IDs do *not* start with any particular value or run in any particular order, so you can't assume for example that they'll be 0, 1, and 2.

The structure of a header packet is then:

| status byte | file ID | file name                           |
|:------------|:--------|:------------------------------------|
| 1 byte      | 1 byte  | the rest of the bytes in the packet |

The structure of a data packet is:

| status byte | file ID | packet number | data                                |
|:------------|:--------|:--------------|:------------------------------------|
| 1 byte      | 1 byte  | 2 bytes       | the rest of the bytes in the packet |

:bangbang: Note that you'll need to look at the length field in the received
packet to figure out how many bytes are in "the rest of the bytes in the
package". Most of the received packets will probably be "full", but the last
packet is likely to be "short". You may assume that the maximum packet size,
however, is 1028 bytes (a data packet with 4 bytes of bookkeeping and 1024
bytes of data).

The decision to only use 1 byte for the file ID means that there can't be more than 256 files being transferred to a given client at a time. Given that the current business plan is to always send exactly three files that shouldn't be a problem, but they'll need to be aware of the limitation if they want to expand the service later.

*Question to think about: Given that we're using 2 bytes for the packet number, and breaking files into 1K chunks, what's the largest file we can transfer using this system?*

### How to construct packet numbers

A data packet has two bytes that specify the packet number, but how do you take
those two bytes and make a packet number out of them? It's "fairly"
straightforward, but there is an annoying complication because Java doesn't
natively support unsigned integer types.

The "simple" part is to realize that we can treat an individual byte as a
"digit" in a base 256 number. So in the same way that 37 is a two digit
decimal number, which we interpret as 3*10 + 7, the pair of bytes:

| most significant byte | least significant byte |
|:----------------------|:-----------------------|
| X                     | Y                      |

as 256*X + Y.

There's a question there of whether the most significant byte comes first
or second; on our protocol, the first byte is the most significant byte as
above. The question of whether the most significant bytes (or bits) come first
or last is important; either works, but it crucial that everyone agrees on
a standard. This is what [arguments about "little endian" and "big endian"
systems](https://en.wikipedia.org/wiki/Endianness) are all about.

**Now for the complication.** In an unsigned universe, a byte can represent
values from 0 to 255. That's really what we're after here; packet numbers
are non-negative, so we'd like to think of X and Y as both non-negative values
so we can just say the packet number is 256*X + Y.

But we can't.

Because Java doesn't support unsigned values. So _it_ thinks a byte represents
the values -128 to 127, and if we just take those values and do 256*X + Y we'll
end up with some messed up values that really won't work.

How do we fix that? First, we're going to _have_ to assign the `byte` value to
an `int` value so we can represent values larger than 127. So we might have
something like:

```java
int x = bytes[2];
int y = bytes[3];
```

(`x` and `y` aren't great names, and you might talk about whether there
are better names.) Now we'll have a value between -128 and 127 that we need
to convert to the range 0 to 255. All the numbers from 0 to 127 are "OK" and
can be left alone. The negative values, though, need to be converted to the
range 128 to 255, with -128 mapping to 128, and -1 mapping to 255. One way to
deal with that would be:

```java
if (x < 0) {
  x += 256
}
if (y < 0) {
  y += 256
}
```

*Do you see why adding 256 to negative values fixes thing?*

(The fact that we have to do the same thing to both of these _strongly_ suggests
that you want a function that converts a `byte` to an unsigned `int` instead of
repeating the logic.)

There's a faster alternative that doesn't involve conditionals that uses bit
masking. We're not going to go into that here, but you might want to
[read about it](https://mkyong.com/java/java-convert-bytes-to-unsigned-bytes)
and give it a try.

As yet another alternative, starting with Java 8 there's
a `Byte.toUnsignedInt()` method that does exactly what we want:

```java
int x = Byte.toUnsignedInt(bytes[2])
int y = Byte.toUnsignedInt(bytes[3])
```

## Writing the client backend

As mentioned above, your (Java) program starts things off by sending a UDP/datagram packet to the server, and then waits and receives packets from the server until all three files are completely received. When a file is complete, it should be written to disk using the file name sent in the header packet. When all three files have been written to disk, the client should terminate cleanly.

### Starting the conversation

You start things off by sending a (mostly empty) packet to the server as a way of saying "Hello – send me stuff!". To do this you'll need to know the name of the server you're connecting to, and the port to use for the connection; this information should be provided in class.

What should that initial packet look like that you send to the server to start
things off? Actually, it can be completely empty, since all you're doing is
announcing that you're interested. The only thing the server needs to respond
to your request is your IP and port number, and all that is encoded in your
outgoing package "for free" by Java's `DatagramPacket` class. So just create an
empty buffer, stick that in a `DatagramPacket` and send it out on the
`DatagramSocket` that you set up between you and the server.

### Processing the packets you receive

The main complication when receiving the packets is we don't control the order in which packets will be received. This means, among other things, that:

* The header packet won't necessarily come first, so we might start receiving
  data for a file before we've gotten the header for it (and know the file
  name). In an extreme case, we might get *all* the data packets (including the
  one with the "last packet" bit set) before we get the header packet.
  (Remember that the "last packet" bit tells us how many packets there should
  be thanks to the packet number, but it doesn't mean that it's the last packet
  to arrive.)
* The data packets can arrive in random order, so we'll have to store them in
  some fashion until we have them all, and then put them in order before we
  write them out to the file.

Other issues include:

* Packets will arrive from all three files interleaved, so we need to make sure
  we can store them sensibly so we can separate out packets for the different
  files.
* We don't know how many packets a file has been split up into until we see the
  packet with the "last packet" bit set.
* You don't know what kind of file they're sending, so you have to make sure to
  handle the data as if it's binary data. You can't _ever_ convert it to strings
  or you'll break things when you try to handle binary data.

Most of this is really a data structures problem. Before you start banging on the keyboard, take some time to talk about how you're going to unmarshall the packets and store their data. Having a good plan for that will make a huge difference.

As far as actually receiving the packages, you just need to keep calling
`socket.receive(packet)` on the `DatagramSocket` you set up until you've got
all the packets. You'll probably want to construct a new `DatagramPacket` for
every call to `receive` so that you know that the receipt of a new packet won't
overwrite the buffer data from the previous packet. Since you know that each
packet has no more than 1024 bytes of data, the buffer in the packet needs to
be big enough for the 1024 bytes of data plus the maximum amount of header
information as discussed in the packet structure description up above.

## Testing

### Unit test your work

:bangbang: ***Write tests*** :bangbang:

While the network stuff is difficult to test, all the parsing and packet/file
assembly logic is entirely testable. I would *strongly* encourage you to write
some tests for that "data structures" part to help define the desired behavior
and identify logic issues. Debugging logic problems when you're interacting
with the actual server will really be a nuisance, so isolating that part as
much as possible would be a Good Idea.

You might, for example, have a
`DataPacket` class (as distinct from the Java library `DatagramPacket` class)
with a constructor that takes an array of bytes. That class could then be
responsible for extracting the status bytes, file ID, packet number, and data,
and store them in fields that are accessible through various `get`
methods. You could then write tests
that construct `DataPackets` and verify that the resulting
`DataPackets` have the correct status bytes, file ID, packet number, and data.

You could also have a `PacketManager` class that you hand packets to and which
manages organizing and storing all the packets. You could then hand it a small
set of test packets that you make up, and verify that it assembles the correct
files. The `PacketManager` could, for example, create `ReceivedFile` objects.
`ReceivedFiles` could contain the packets for a file, and have getter methods
for the file name, the number of packets (what if it isn't known yet?), the
number actually received, whether the file is complete, and the data from those
packets after sorting them in the correct order.

All of these ideas are just that: ideas. Your group should definitely spend
some time discussing how you want to organize all this, and how you want to
test that. If you're not clear on how you'd structure something for testing,
*come ask* rather than just banging out a bunch of code that will just confuse
us all later.

You don't have to do things like test `DatagramSocket` or file writing. The
Java folks are responsible for the correctness of `DatagramSocket` and we'll
trust them on that. Testing that you're writing out the correct files is
essentially handled in the `bats` tests below, so don't bother trying to do
JUnit things about that.

### Check your work by running your client by hand

In addition to your unit tests, you can run your program "by hand" and see if
the files you get back match the expected files. There's a script `run_client.sh`
in `src`; go into `src` and run

```bash
./run_client.sh <server>
```

Here you replace `<server>` with `localhost` if you want to run your client
against a copy of the server running on your own machine. If you replace
`<server>` with the name of the lab box running a copy of the server provided
by the instructor, then it will run your client against a copy of that
remote server.

If your client is working correctly, this script should terminate gracefully,
if slowly (it's taking nearly a minute for me), leaving three files in
the directory you ran it in:

* `small.txt`
* `AsYouLikeIt.txt`
* `binary.jpg`

The `test/target-files` folder in the repository contains three files with
the same names – these are copies of the expected files and the files your
program downloaded should match these three files exactly.
So, for example, (assuming you're still in `src`), running a command like this

```bash
diff binary.jpg ../test/target-files/binary.jpg
```

should return no differences. You should also be able to examine the contents
of the files you received and assembled and confirm that they look reasonable.

A common problem is that you didn't write the last few bytes of data to the
file. This might show up as the long text file missing the last few characters
or lines. In `binary.jpg` this might show up as some black pixels in the bottom
right of the image.

### Check your work using `bats` tests

There's a (quite simplistic) `bats` test that you can use to run your client
and check that the files you get match the expected files. Run it from the
top-level directory, i.e.,

```bash
bats test/client_tests.bats
```

It basically does the "hand test" described above against your local server,
and `diff`s the files you downloaded against the three expected files.

If these pass, then your code is probably in good shape from a correctness
standpoint, but you should still make sure you have reasonable JUnit tests
and clean, well-organized code.
