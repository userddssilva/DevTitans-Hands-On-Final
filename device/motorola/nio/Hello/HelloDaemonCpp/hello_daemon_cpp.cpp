#include "hello_daemon_cpp.h"

using namespace std;                    // Permite usar o cout e endl diretamente ao invés de std::cout

namespace devtitans::hello {            // Entra no pacote devtitans::hello

void HelloDaemonCpp::startDaemon() {    // Implementa o método startDaemon da classe HelloDaemonCpp
    ALOG(LOG_INFO, "DevTITANS", "Daemon Hello iniciando ...");

    int count = 1;
    while (true) {

        // Implemente aqui o serviço

        ALOG(LOG_INFO, "DevTITANS", "Daemon Hello World loop %d", count++);
        sleep(5);
    }

    // Nunca deve chegar aqui ...
}

} // namespace


using namespace devtitans::hello;       // Permite usar HelloDaemonCpp diretamente ao invés de devtitans::hello::HelloDaemonCpp

int main() {
    HelloDaemonCpp daemon;              // Variável daemon, da classe HelloDaemonCpp, do pacote devtitans::hello
    daemon.startDaemon();               // Executa o método startDaemon
    // Nunca deve chegar aqui ...
    return 0;
}
