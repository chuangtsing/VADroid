default:
	protoc vanet_pb.proto --java_out=src/main/java
fetch:
	rm vanet_pb.proto || true
	wget https://raw.githubusercontent.com/zblassman/va_protobuf/master/vanet_pb.proto
	protoc vanet_pb.proto --java_out=src/main/java
