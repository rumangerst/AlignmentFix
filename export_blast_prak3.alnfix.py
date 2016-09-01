# Export AlignmentFix edits to a BLAST output table
# For non-standard BLAST table, the first line must be #type1  type2   type3 ... to let this script know the mappings
# The BLAST table must contain qseqid, sseqid, sstart and ssend
# Alignment IDs must be formated like qseqid|genome_scaffold_sstart_send
# sseqid=genome_scaffold

# Usage: [] --alnfix <AlignmentFix instructions> --input <BLAST table> --output <Output file>

import sys

def find_matching_entries(id, data, refine):
	
	result = []
	
	target_qseqid = id.split("|")[1]
	target_sseqid = id.split("|")[0].split("_")[0] + "_" + id.split("|")[0].split("_")[1]
	target_sstart = int(id.split("|")[0].split("_")[2])
	target_send = int(id.split("|")[0].split("_")[3])
	
	# First try 
	
	for entry in data:
		
		entry_qseqid = entry["qseqid"]
		entry_sseqid = entry["sseqid"]
		entry_sstart = int(entry["sstart"])
		entry_send = int(entry["send"])
		
		if target_qseqid == entry_qseqid and target_sseqid == entry_sseqid:
			
			# refinement is needed for unique hits
			tstart = target_sstart
			tend = target_send
			estart = entry_sstart
			eend = entry_send
			
			if tstart > tend:
				(tstart, tend) = (tend, tstart) #python yay
			if estart > eend:
				(estart, eend) = (eend, estart) #python yay
			
			# If refinement, the hit should be within the region provided by the id
			if not refine or ( estart >= tstart and eend <= tend ):
				result.append(entry)
		
	return result
	
def fix_entry(algnfix, entry, log):
	
	qseqid = entry["qseqid"]
	sseqid = entry["sseqid"]
	
	sstart = int(entry["sstart"])
	send = int(entry["send"])
	left = algnfix["left"]
	right = algnfix["right"]
	
	# fix sstart
	
	if sstart < send:
		sstart = sstart - left
	else:
		sstart = sstart + left
		
	# fix send
	
	if sstart < send:
		send = send + right
	else:
		send = send - right
		
	# Put into entry
	original_sstart = entry["sstart"]
	original_send = entry["send"]
	
	entry["sstart"] = str(sstart)
	entry["send"] = str(send)
	
	log.append("\t".join( [ qseqid, sseqid, original_sstart + "->" + str(sstart), original_send + "->" + str(send), "EXTEND_LEFT_" + str(left), "EXTEND_RIGHT_" + str(right)] ))
	

def fix(algnfix, data):
	
	log = []
	
	for id in algnfix:
		
		entries = find_matching_entries(id, data, False)
		
		if not entries:
			raise RuntimeError("Cannot find matching entry for " + id)
			
		if len(entries) > 1:
			entries = find_matching_entries(id, data, True)
			
		if len(entries) != 1:
			raise RuntimeError("Cannot find unique matching entry for " + id)
			
		entry = entries[0]
		
		# The actual calculation
		fix_entry(algnfix[id], entry, log)
		
	return log

def process(input_file, output_file, algnfix_file):
	
	# Read input file
	f = open(input_file, "r")
	
	content = f.read().split("\n")
	mapping = [ "qseqid", "sseqid", "pident", "length", "mismatch", "gapopen", "qstart", "qend", "sstart", "send", "evalue", "bitscore" ]
	data = []
	
	if content[0].startswith("#"):
		mapping = list(content[0][1:].strip().split("\t"))
		
		print("Changed mapping to " + ", ".join(mapping))
		
		
	for _line in content:
		
		line = _line.strip()
		
		if not line:
			continue
		if line.startswith("#"):
			continue
		
		entry = {}
		
		cell = line.split("\t")
		
		for i in range(len(mapping)):
			
			entry[mapping[i]] = cell[i]
			
		data.append(entry)
	
	f.close()
	
	# Read alignment fixer file
	algnfix = {}
	
	f = open(algnfix_file, "r")
	
	for _line in f:
		
		line = _line.strip()
		
		if not line:
			continue
		if line.startswith("#"):
			continue
			
		cell = line.split("\t")
		
		(id, eleft, eright) = cell[:3]
		algnfix[id] = { "left" : int(eleft), "right" : int(eright) }
	
	f.close()
	
	# Fix the data
	log = fix(algnfix, data)
	print("\n".join(log))
		
	
	# Write the data
	f = open(output_file, "w")
	
	f.write("#" + "\t".join(mapping))
	
	for entry in data:
		
		f.write("\n" + "\t".join( [ entry[k] for k in mapping ] )) # python yay
		
	
	f.write("\n" + "\n# ".join(log))
	
	f.close()

def main():
	
	mode = ""
	_algnfix = ""
	_input = ""
	_output = ""
	
	print(sys.argv)
	
	for arg in sys.argv[1:]:
		
		if arg == "--alnfix":
			mode = "alnfix"
		elif arg == "--input":
			mode = "input"
		elif arg == "--output":
			mode = "output"
		else:
						
			if mode == "input":
				_input = arg
			elif mode == "alnfix":
				_algnfix = arg
			elif mode == "output":
				_output = arg
			else:
				raise RuntimeError("Set --alnfix or --input or --output before setting a file!")	
				
	if not _algnfix:
		raise RuntimeError("Set AlignmentFix file with --alnfix")
	if not _input:
		raise RuntimeError("Set input with --input")
	if not _output:
		raise RuntimeError("Set output with --output")
				
	print("Following input file will be used: " + _input)
	print("Following AlignmentFix file will be used: " + _algnfix)
	print("Output will be written to " + _output)
	
	process(_input, _output, _algnfix)
	
main()

