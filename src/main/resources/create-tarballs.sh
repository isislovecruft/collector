#!/bin/bash
#
# Copyright 2016 The Tor Project
# See LICENSE for licensing information.
#
# Script for creating descriptor tarballs on a CollecTor instance,
# and creating/touching the symlinks for the web-app.
#
####
# Configuration section:
# The following path should be adjusted, if the CollecTor server layout differs.
# OUTDIR and TARBALLTARGETDIR have to be given absolute or relative to WORKDIR.
ARCHIVEDIR="archive"
WORKDIR="tarballs"
OUTDIR="../out"
TARBALLTARGETDIR="../data"
### end of configuration section.
#
### script start
echo `date` "Starting"
YEARONE=`date +%Y`
MONTHONE=`date +%m`
YEARTWO=`date --date='7 days ago' +%Y`
MONTHTWO=`date --date='7 days ago' +%m`
CURRENTPATH=`pwd`

if ! test -d $WORKDIR
  then mkdir $WORKDIR
fi

cd $WORKDIR

if ! test -d $OUTDIR
  then echo "$OUTDIR doesn't exist.  Exiting."
  exit 1
fi

if ! test -d $TARBALLTARGETDIR
  then echo "$TARBALLTARGETDIR doesn't exist.  Exiting."
  exit 1
fi

TARBALLS=(
  exit-list-$YEARONE-$MONTHONE
  exit-list-$YEARTWO-$MONTHTWO
  torperf-$YEARONE-$MONTHONE
  torperf-$YEARTWO-$MONTHTWO
  certs
  microdescs-$YEARONE-$MONTHONE
  microdescs-$YEARTWO-$MONTHTWO
  consensuses-$YEARONE-$MONTHONE
  consensuses-$YEARTWO-$MONTHTWO
  votes-$YEARONE-$MONTHONE
  votes-$YEARTWO-$MONTHTWO
  server-descriptors-$YEARONE-$MONTHONE
  server-descriptors-$YEARTWO-$MONTHTWO
  extra-infos-$YEARONE-$MONTHONE
  extra-infos-$YEARTWO-$MONTHTWO
  bridge-descriptors-$YEARONE-$MONTHONE
  bridge-descriptors-$YEARTWO-$MONTHTWO
)
TARBALLS=($(printf "%s\n" "${TARBALLS[@]}" | uniq))

DIRECTORIES=(
  $OUTDIR/exit-lists/$YEARONE/$MONTHONE/
  $OUTDIR/exit-lists/$YEARTWO/$MONTHTWO/
  $OUTDIR/torperf/$YEARONE/$MONTHONE/
  $OUTDIR/torperf/$YEARTWO/$MONTHTWO/
  $OUTDIR/relay-descriptors/certs/
  $OUTDIR/relay-descriptors/microdesc/$YEARONE/$MONTHONE
  $OUTDIR/relay-descriptors/microdesc/$YEARTWO/$MONTHTWO
  $OUTDIR/relay-descriptors/consensus/$YEARONE/$MONTHONE
  $OUTDIR/relay-descriptors/consensus/$YEARTWO/$MONTHTWO
  $OUTDIR/relay-descriptors/vote/$YEARONE/$MONTHONE/
  $OUTDIR/relay-descriptors/vote/$YEARTWO/$MONTHTWO/
  $OUTDIR/relay-descriptors/server-descriptor/$YEARONE/$MONTHONE/
  $OUTDIR/relay-descriptors/server-descriptor/$YEARTWO/$MONTHTWO/
  $OUTDIR/relay-descriptors/extra-info/$YEARONE/$MONTHONE/
  $OUTDIR/relay-descriptors/extra-info/$YEARTWO/$MONTHTWO/
  $OUTDIR/bridge-descriptors/$YEARONE/$MONTHONE/
  $OUTDIR/bridge-descriptors/$YEARTWO/$MONTHTWO/
)
DIRECTORIES=($(printf "%s\n" "${DIRECTORIES[@]}" | uniq))

for (( i = 0 ; i < ${#TARBALLS[@]} ; i++ )); do
  if [ ! -d ${TARBALLS[$i]} ]; then
    echo `date` "Creating symlink for" ${TARBALLS[$i]} 
    ln -s ${DIRECTORIES[$i]} ${TARBALLS[$i]}
  else
    # This is a workaround for the "tar u" bug in GNU tar 1.20
    echo `date` "Touching symlink and directories for" ${TARBALLS[$i]} 
    find -L ${TARBALLS[$i]} -type d | xargs touch
  fi
done

for (( i = 0 ; i < ${#TARBALLS[@]} ; i++ )); do
  echo `date` "Creating" ${TARBALLS[$i]}'.tar'
  tar chf ${TARBALLS[$i]}.tar ${TARBALLS[$i]}
  if [ ! -f ${TARBALLS[$i]}.tar.xz ]; then
    echo `date` "Compressing" ${TARBALLS[$i]}'.tar'
    xz -9e ${TARBALLS[$i]}.tar
  fi
done

echo `date` "Moving tarballs into place"
mv *.tar.xz $TARBALLTARGETDIR

cd $CURRENTPATH

echo `date` "Finished tarball creation.  Starting symlink-update ..."

mkdir -p $ARCHIVEDIR/bridge-descriptors/
ln -f -s -t $ARCHIVEDIR/bridge-descriptors/ $TARBALLTARGETDIR/bridge-descriptors-20??-??.tar.xz

mkdir -p $ARCHIVEDIR/bridge-pool-assignments/
ln -f -s -t $ARCHIVEDIR/bridge-pool-assignments/ $TARBALLTARGETDIR/bridge-pool-assignments-20??-??.tar.xz

mkdir -p $ARCHIVEDIR/exit-lists/
ln -f -s -t $ARCHIVEDIR/exit-lists/ $TARBALLTARGETDIR/exit-list-20??-??.tar.xz

mkdir -p $ARCHIVEDIR/relay-descriptors/
ln -f -s -t $ARCHIVEDIR/relay-descriptors/ $TARBALLTARGETDIR/certs.tar.xz

mkdir -p $ARCHIVEDIR/relay-descriptors/consensuses/
ln -f -s -t $ARCHIVEDIR/relay-descriptors/consensuses/ $TARBALLTARGETDIR/consensuses-20??-??.tar.xz

mkdir -p $ARCHIVEDIR/relay-descriptors/extra-infos/
ln -f -s -t $ARCHIVEDIR/relay-descriptors/extra-infos/ $TARBALLTARGETDIR/extra-infos-20??-??.tar.xz

mkdir -p $ARCHIVEDIR/relay-descriptors/microdescs/
ln -f -s -t $ARCHIVEDIR/relay-descriptors/microdescs/ $TARBALLTARGETDIR/microdescs-20??-??.tar.xz

mkdir -p $ARCHIVEDIR/relay-descriptors/server-descriptors/
ln -f -s -t $ARCHIVEDIR/relay-descriptors/server-descriptors/ $TARBALLTARGETDIR/server-descriptors-20??-??.tar.xz

mkdir -p $ARCHIVEDIR/relay-descriptors/statuses/
ln -f -s -t $ARCHIVEDIR/relay-descriptors/statuses/ $TARBALLTARGETDIR/statuses-20??-??.tar.xz

mkdir -p $ARCHIVEDIR/relay-descriptors/tor/
ln -f -s -t $ARCHIVEDIR/relay-descriptors/tor/ $TARBALLTARGETDIR/tor-20??-??.tar.xz

mkdir -p $ARCHIVEDIR/relay-descriptors/votes/
ln -f -s -t $ARCHIVEDIR/relay-descriptors/votes/ $TARBALLTARGETDIR/votes-20??-??.tar.xz

mkdir -p $ARCHIVEDIR/torperf/
ln -f -s -t $ARCHIVEDIR/torperf/ $TARBALLTARGETDIR/torperf-20??-??.tar.xz

echo `date` "Finished."