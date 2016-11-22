@echo on

call C:\android\android-ndk-r10c\ndk-build -j 8 USE_BREAKPAD=0
cd libs
mkdir ..\assets
mkdir ..\build\

for /D %%f in (*) do (
	copy %%f\pievpn ..\assets\minivpn.%%f
	del %%f\libcrypto.so
	del %%f\libssl.so

	mkdir ..\build\native-libs\%%f\
	copy %%f\*.so  ..\build\native-libs\%%f\
)

cd ..