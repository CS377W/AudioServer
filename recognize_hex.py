import sys
sys.path.insert(0, './dejavu')
import numpy as np
from dejavu import Dejavu
import warnings
import json
from dejavu.fingerprint import DEFAULT_FS, DEFAULT_WINDOW_SIZE, DEFAULT_OVERLAP_RATIO
warnings.filterwarnings("ignore")

with open("dejavu.cnf") as f:
    config = json.load(f)

djv = Dejavu(config)

with open('./input.txt', 'r') as content_file:
    inHex = content_file.read().strip().decode('string-escape')
    # print len(inHex)

    data = np.fromstring(inHex, np.int16)

    # print len(data)
    # print data[:128]

    matches = []
    matches.extend(djv.find_matches(data))
    # print matches
    song = djv.align_matches(matches)

    result = None
    if song is not None:
      result = dict()
      result["song_name"] = song["song_name"]
      result["offset"] = str(song["offset"])

      offset = song["offset"]
      result["seconds"] = (offset *
          (DEFAULT_WINDOW_SIZE - DEFAULT_WINDOW_SIZE*DEFAULT_OVERLAP_RATIO) +
          DEFAULT_WINDOW_SIZE*DEFAULT_OVERLAP_RATIO) / DEFAULT_FS

    print json.dumps(result)

