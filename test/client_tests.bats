#!/usr/bin/env bats

# Run this from the top level directory, i.e.,
# bats test/client_tests.bats

# We only want to run the client once at the start because
# it takes quite a while, so the `setup` work only happens
# one time.
setup(){
    if [ "$BATS_TEST_NUMBER" -eq 1 ]; then
      cd test/lib || exit
      java -jar Segmented-File-System-server.jar &
      sleep 1

      cd ../../src || exit

      # Clean out any previously downloaded files.
      rm -f small.txt
      rm -f AsYouLikeIt.txt
      rm -f binary.jpg

      # (Re)compile the code
      rm -f segmentedfilesystem/*.class
      javac segmentedfilesystem/*.java

      # Run the client
      java segmentedfilesystem.Main localhost

      cd .. || exit

      kill %1
    fi
}


@test "Your client correctly assembled small.txt" {
  # Uncomment this line if you want to see the result of
  # the diff if this test is failing. Similar lines can
  # help with the other tests.
  # diff test/target-files/small.txt src/small.txt
  run diff test/target-files/small.txt src/small.txt

  [ "$status" -eq 0 ]
}

@test "Your client correctly assembled AsYouLikeIt.txt" {
  run diff test/target-files/AsYouLikeIt.txt src/AsYouLikeIt.txt

  [ "$status" -eq 0 ]
}

@test "Your client correctly assembled binary.jpg" {
  run diff test/target-files/binary.jpg src/binary.jpg

  [ "$status" -eq 0 ]
}
