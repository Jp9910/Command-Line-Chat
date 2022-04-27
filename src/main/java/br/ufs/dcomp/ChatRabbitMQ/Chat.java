package br.ufs.dcomp.ChatRabbitMQ;

import com.rabbitmq.client.*;
import java.io.IOException;

import java.util.Scanner;
import java.time.LocalDate;
import java.time.LocalTime;
import java.io.File;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.nio.file.*;
import com.google.protobuf.ByteString;

public class Chat {

  private static Scanner sc = new Scanner(System.in);
  private static String prompt = new String("");
  private static String USUARIO = "";
  private static String pessoaDest = "";
  private static String grupoDest = "";
  private static boolean escolheuDestinatario = false;
  private static Channel channelT;
  private static Channel channelF;
  
  public static void main(String[] argv) throws Exception 
  {
    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost("3.227.213.73");
    factory.setUsername("jp");
    factory.setPassword("9910");
    factory.setVirtualHost("/");
    Connection connection = factory.newConnection();
    
    channelT = connection.createChannel(); //canal para mensagens de texto
    channelF = connection.createChannel(); //canal para mensagens de arquivo
    
    while(USUARIO.isEmpty())
    {
        System.out.print("User: ");
        USUARIO = sc.nextLine();
    }
                      //(queue-name, durable, exclusive, auto-delete, params); 
    channelT.queueDeclare(USUARIO+"-T", false,   false,     false,       null); //Fila para mensagens de texto
    channelF.queueDeclare(USUARIO+"-F", false,   false,     false,       null); //Fila para mensagens de arquivo
    
    //declarar comportamento da função de recebimento da fila de textos
    Consumer consumerT = new DefaultConsumer(channelT) 
    {
      public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException 
      {
          // O que o receptor fará quando chegar uma mensagem. "body" é o conteúdo da mensagem (representado em bytes)
          // Desserializar o conteúdo recebido
          MensagemProto.Mensagem mensagem_recebida = MensagemProto.Mensagem.parseFrom(body);
          String emissor = mensagem_recebida.getEmissor();
          String data = mensagem_recebida.getData();
          String hora = mensagem_recebida.getHora();
          String grupo = mensagem_recebida.getGrupo();
          
          MensagemProto.Conteudo conteudo = mensagem_recebida.getConteudo();
          String texto = conteudo.getCorpo().toStringUtf8();
          
          // Formato da mensagem para usuário: (21/09/2016 às 20:53) marciocosta diz: E aí, Tarcisio! Vamos sim!
          // Formato da mensagem para grupo:   (21/09/2018 às 21:50) joaosantos#amigos diz: Olá, amigos!!!
          String msg_formatada = "";
          if(grupo.isEmpty())
              msg_formatada = (  "("+data+" às "+hora+") "+emissor+" diz: "+texto   );
          else
              msg_formatada = (  "("+data+" às "+hora+") "+emissor+"#"+grupo+" diz: "+texto   );
          System.out.println("\n"+msg_formatada);
          System.out.print(prompt);
      }
    };
    
    //declarar comportamento da função de recebimento da fila de arquivos
    Consumer consumerF = new DefaultConsumer(channelF) 
    {
        public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException 
        {
            // O que o receptor fará quando chegar uma mensagem. "body" é o conteúdo da mensagem (representado em bytes)
            
            MensagemProto.Mensagem mensagem_recebida = MensagemProto.Mensagem.parseFrom(body);
            String emissor = mensagem_recebida.getEmissor();
            String data = mensagem_recebida.getData();
            String hora = mensagem_recebida.getHora();
            String grupo = mensagem_recebida.getGrupo();
            
            MensagemProto.Conteudo conteudo = mensagem_recebida.getConteudo();
            byte[] conteudoArquivoRecebido = conteudo.getCorpo().toByteArray();
            String nomeArquivoRecebido = conteudo.getNome();
            String tipoMimeArquivoRecebido = conteudo.getTipo();
            // Criar um novo arquivo e salvar o vetor de bytes nele
            System.out.println("\nRecebendo Arquivo...");
            String caminhoNovoArquivo = "ArquivosRecebidos" + File.separator + USUARIO + File.separator + nomeArquivoRecebido;
            try {
                File novoArquivo = new File(caminhoNovoArquivo);
                novoArquivo.getParentFile().mkdirs();
                if (novoArquivo.createNewFile())
                    System.out.println("Arquivo criado: " + novoArquivo.getName());
                else
                    System.out.println("Arquivo já existe. Escrevendo conteúdo recebido nele.");
                OutputStream escreverArquivo = new FileOutputStream(caminhoNovoArquivo);
                escreverArquivo.write(conteudoArquivoRecebido);
                escreverArquivo.close();
                System.out.println("Arquivo escrito.");
            } catch (IOException e) {
                  System.out.println(" !! Erro no recebimento de arquivo. !! ");
                  //e.printStackTrace();
                  System.out.print(prompt);
            }
            
            String msg_formatada = "";
            if(grupo.isEmpty())
                msg_formatada = (  "("+data+" às "+hora+") Arquivo \""+nomeArquivoRecebido+"\" recebido de @"+emissor   );
            else
                msg_formatada = (  "("+data+" às "+hora+") Arquivo \""+nomeArquivoRecebido+"\" recebido de @"+emissor+"#"+grupo   );
            System.out.println(msg_formatada);
            System.out.print(prompt);
        }
    };
    
    ThreadRecebimento threadT = new ThreadRecebimento("threadT", channelT, consumerT, USUARIO, "-T");
    ThreadRecebimento threadF = new ThreadRecebimento("threadF", channelF, consumerF, USUARIO, "-F");
    threadT.start();
    threadF.start();
    
    Chat.imprimirAjuda();
    Chat.imprimirComandos();
    prompt = "["+USUARIO+"] >> ";
    while(true) //iniciar o chat
    {
        Chat.proximaOperacao();
    }
  }
  
  public static void proximaOperacao() throws java.io.IOException
  {
      System.out.print(prompt);
      String operacao = sc.nextLine();
      if(!operacao.isEmpty()) //se a operação não for vazia
      {
          char primeirochar = operacao.charAt(0);
          switch (primeirochar)
          {
            case '@': //operação tipo trocar para mensagem para pessoa
                  if(operacao.substring(1).equals(USUARIO)){
                    System.out.println("Não é possível enviar mensagens para sí mesmo.");
                    break;
                  }
                  pessoaDest = operacao.substring(1);
                  grupoDest = "";
                  prompt = "["+USUARIO+"] @"+pessoaDest+">> ";
                  System.out.println("Enviando mensagens para "+pessoaDest+".");
                  escolheuDestinatario = true;
                  break;
            
            case '!': //operação tipo comando
                  String[] argumentos = operacao.split(" ");
                  String comando = argumentos[0].substring(1);
                  if(comando.equals("addGroup")) //criar novo grupo (exchange do tipo ~~fanout~~ topic)
                  {
                      if(argumentos.length == 2)
                      {
                          String nomeDoGrupo = argumentos[1];
                          channelT.exchangeDeclare(nomeDoGrupo,"topic");
                          channelT.queueBind(USUARIO+"-T", nomeDoGrupo, "-T");
                          channelF.queueBind(USUARIO+"-F", nomeDoGrupo, "-F");
                      }
                      else
                          System.out.println("Comando inválido.");
                  }
                  else if(comando.equals("addUser")) //adicionar usuário ao grupo
                  {
                      if(argumentos.length == 3)
                      {
                          String novoIntegrante = argumentos[1];
                          String nomeDoGrupo = argumentos[2];
                          try {
                              channelT.queueBind(novoIntegrante+"-T", nomeDoGrupo, "-T");
                              channelF.queueBind(novoIntegrante+"-F", nomeDoGrupo, "-F");
                          } catch (IOException ioe) {
                              System.out.print("Grupo ou usuário não existe.");
                              ioe.printStackTrace();
                              System.out.print(prompt);
                          } catch (AlreadyClosedException ace) {
                              System.out.println(" !! Erro no canal. Reinicie o chat !! ");
                              ace.printStackTrace();
                              System.out.print(prompt);
                          }
                      }
                      else {System.out.println("Comando inválido.");}
                  }
                  else if(comando.equals("delFromGroup")) //deletar usuário do grupo
                  {
                      if(argumentos.length == 3)
                      {
                          String removerIntegrante = argumentos[1];
                          String nomeDoGrupo = argumentos[2];
                          try {
                              channelT.queueUnbind(removerIntegrante+"-T", nomeDoGrupo, "-T");
                              channelF.queueUnbind(removerIntegrante+"-F", nomeDoGrupo, "-F");
                          } catch (IOException ioe) {
                              System.out.print("Grupo ou usuário não existe.");
                              ioe.printStackTrace();
                              System.out.print(prompt);
                          } catch (AlreadyClosedException ace) {
                              System.out.println(" !! Erro no canal. Reinicie o chat !! ");
                              ace.printStackTrace();
                              System.out.print(prompt);
                          }
                      }
                      else
                          System.out.println("Comando inválido.");
                  }
                  else if(comando.equals("removeGroup")) //deletar grupo
                  {
                      if(argumentos.length == 2)
                          channelT.exchangeDelete(argumentos[1]);
                      else
                          System.out.println("Comando inválido.");
                  }
                  else if(comando.equals("upload")) // upload de arquivo
                  {
                      if(argumentos.length == 2)
                          if(escolheuDestinatario)
                              Chat.enviarArquivo(argumentos[1]);
                          else
                              System.out.println("Escolha o grupo ou usuário destinatário.");
                      else
                          System.out.println("Comando inválido.");
                  }
                  else
                      System.out.println("Comando inválido.");
                  break;
            
            case '#': //operação tipo trocar para mensagem para grupo
                  grupoDest = operacao.substring(1);
                  pessoaDest = "";
                  prompt = "["+USUARIO+"] #"+grupoDest+">> ";
                  System.out.println("Enviando mensagens para o grupo "+grupoDest+".");
                  escolheuDestinatario = true;
                  break;
            
            default: //operação tipo enviar mensagem de texto
                  /*
                  * Montar o protocol buffer. O arquivo "MensagemProto.java" contém todas as 'messages' (classe Mensagem e classe Conteúdo).
                  * Builders têm getters e setters. Servem para preencher o objeto. Usar .build() para transformar em uma Message.
                  * Messages têm apenas getters e são imutáveis.
                  */
                  if(escolheuDestinatario)
                  {
                    Chat.enviarTexto(operacao);
                  }
                  else{System.out.println("Operação inválida.");}
          }
      }
  }
  
  /*
  * Função para criar uma thread que fará o envio de arquivo como mensagem
  */
  private static void enviarArquivo(String caminho)
  {
      ThreadEnvio thEnvio = new ThreadEnvio("thEnvio",channelF,USUARIO,pessoaDest,grupoDest,"-F",caminho);
      thEnvio.start();
  }
  
  /*
  * Função para enviar texto como mensagem
  */
  private static void enviarTexto(String texto)
  {
      boolean isGroupMessage = pessoaDest.isEmpty();
      MensagemProto.Mensagem mensagem_construida;
      if(isGroupMessage)
        mensagem_construida = Chat.MontarMensagemTexto(texto,USUARIO,grupoDest);
      else
        mensagem_construida = Chat.MontarMensagemTexto(texto,USUARIO, null);
      
      //Serializar a Mensagem em um vetor de bytes para depois enviar
      byte[] mensagem_serializada = mensagem_construida.toByteArray();
      
      try {
          if(isGroupMessage)
              channelT.basicPublish(grupoDest,"-T",null,mensagem_serializada); //mensagem para grupo
          else
              channelT.basicPublish("",pessoaDest+"-T",null,mensagem_serializada); //mensagem para pessoa
      } catch (IOException ioe) {
          System.out.println(" !! Erro no publish. Não foi possível enviar a mensagem. Tente Novamente. !! ");
          ioe.printStackTrace();
          System.out.print(prompt);
      } catch (AlreadyClosedException ace){
          System.out.println(" !! Erro no canal. Reinicie o chat !! ");
          ace.printStackTrace();
          System.out.print(prompt);
      }
  }
  
  /*
  * Função auxiliar para montar uma mensagem de texto que será enviada para um usuário usando protocol buffers
  */
  public static MensagemProto.Mensagem MontarMensagemArquivo(String caminho, String emissor, String grupoDest)
  {
      Path sourceArquivo = Paths.get(caminho); // Carregar o arquivo
      String nomeArquivo = sourceArquivo.getFileName().toString(); // Pegar o nome do arquivo
      String tipoMime;
      byte[] conteudoArquivo;
      try {
          tipoMime = Files.probeContentType(sourceArquivo); // Pegar o tipo mime do arquivo
          conteudoArquivo = Files.readAllBytes(sourceArquivo); // Carregar o arquivo como vetor de bytes
      } catch (IOException ioe) {
          System.out.println(" !! Erro ao carregar arquivo. Verifique se o caminho está correto. !! ");
          //ioe.printStackTrace();
          System.out.print(prompt);
          return null;
      }
    
      //Montar o conteúdo da mensagem (classe Conteúdo)
      MensagemProto.Conteudo.Builder conteudo = MensagemProto.Conteudo.newBuilder();
      conteudo.setTipo(tipoMime)
              .setCorpo(ByteString.copyFrom(conteudoArquivo))
              .setNome(nomeArquivo);
      
      String data = Chat.getData();
      String hora = Chat.getHora();
      
      //Montar a mensagem (classe Mensagem), definindo o conteúdo para o montado acima.
      MensagemProto.Mensagem.Builder mensagem = MensagemProto.Mensagem.newBuilder();
      if(grupoDest != null)
        mensagem.setGrupo(grupoDest);
      MensagemProto.Mensagem mensagem_construida = mensagem.setEmissor(emissor)
                                                            .setData(data)
                                                            .setHora(hora)
                                                            .setConteudo(conteudo)
                                                            .build();
      
      return mensagem_construida;
  }
  
  /*
  * Função auxiliar para montar uma mensagem de texto que será enviada usando protocol buffers
  */
  public static MensagemProto.Mensagem MontarMensagemTexto(String texto, String emissor, String grupoDest)
  {
      //Montar o conteúdo da mensagem (classe Conteúdo)
      MensagemProto.Conteudo.Builder conteudo = MensagemProto.Conteudo.newBuilder();
      conteudo.setTipo("text/plain")
              .setCorpo(ByteString.copyFromUtf8(texto));
      
      String data = Chat.getData();
      String hora = Chat.getHora();
      
      //Montar a mensagem (classe Mensagem), definindo o conteúdo para o montado acima.
      MensagemProto.Mensagem.Builder mensagem = MensagemProto.Mensagem.newBuilder();
      if(grupoDest != null)
        mensagem.setGrupo(grupoDest);
      MensagemProto.Mensagem mensagem_construida = mensagem.setEmissor(emissor)
                                                            .setData(data)
                                                            .setHora(hora)
                                                            .setConteudo(conteudo)
                                                            .build();
      return mensagem_construida;
  }
  
  /*
  * Threads usam para imprimir o prompt atual
  */
  public static String getPrompt()
  {
    return prompt;
  }

  /*
  * Função auxiliar para retornar a hora atual já formatada para a troca de mensagens
  */
  private static String getHora()
  {
      LocalTime hora = java.time.LocalTime.now();
      String hora_formatada = String.valueOf(hora.getHour()) +":"+ String.valueOf(hora.getMinute());
      
      return hora_formatada;
  }
  
  /*
  * Função auxiliar para retornar a data atual já formatada para a troca de mensagens
  */
  private static String getData()
  {
      LocalDate data = java.time.LocalDate.now();
      int mes = data.getMonthValue();
      int dia = data.getDayOfMonth();
      String mes_formatado = mes < 10? "0"+String.valueOf(mes) : String.valueOf(mes);
      String dia_formatado = dia < 10? "0"+String.valueOf(dia) : String.valueOf(dia);
      String data_formatada = dia_formatado+"/"+mes_formatado+"/"+String.valueOf(data.getYear());
      
      return data_formatada;
  }
  
  /*
  * Função auxiliar imprimir a lista de operações
  */
  private static void imprimirAjuda()
  {
      System.out.println("+----------------------------LISTA DE OPERAÇÕES-----------------------------+");
      System.out.println("| Digite '@<destinatário>' para enviar mensagem para uma pessoa. Ex: @joao  |");
      System.out.println("| Digite '!<comando>' para executar um comando. Ex: !addGroup amigos        |");
      System.out.println("| Digite '#<grupo>' para enviar mensagem para um grupo. Ex: #amigos         |");
      System.out.println("+---------------------------------------------------------------------------+");
  }
  
  /*
  * Função auxiliar imprimir a lista de comandos
  */
  private static void imprimirComandos()
  {
      System.out.println("/------------------------------------------------LISTA DE COMANDOS-------------------------------------------------\\");
      System.out.println("| Digite '!addGroup <nome_do_grupo>' para criar um novo grupo. Ex: !addGroup amigos                                |");
      System.out.println("| Digite '!addUser <usuario> <nome_do_grupo>' para adicionar um usuário a um grupo. Ex: !addUser joao amigos       |");
      System.out.println("| Digite '!delFromGroup <usuário> <nome_do_grupo>' para remover um usuário do grupo. Ex: !delFromGroup joao amigos |");
      System.out.println("| Digite '!removeGroup <nome_do_grupo>' para deletar um grupo. Ex: !removeGroup amigos                             |");
      System.out.println("| Digite '!upload <caminho_ate_o_arquivo>' para enviar um arquivo ao destinatário atual                            |");
      System.out.println("\\------------------------------------------------------------------------------------------------------------------/");
  }
}
//!upload /home/ubuntu/environment/chat-em-linha-de-comando-via-rabbitmq-Jp9910/ArquivosParaEnviar/arquivoteste.txt
//!upload /home/ubuntu/environment/chat-em-linha-de-comando-via-rabbitmq-Jp9910/ArquivosParaEnviar/livro.pdf

//To do:
//criar threads para o recebimento -- ok
//criar threads para o envio
//criar um canal para texto e um canal para arquivos (pode usar o mesmo canal para consumir e para enviar - 1 canal para consumir e enviar texto, e 1 canal para consumir e enviar arquivos)
//criar fila para textos e fila para arquivos -- ok
//criar exchange do tipo Topic. Esse exchange usa uma tag para escolher a fila para que vai mandar a mensagem (que pode ser texto ou arquivo)

// <!-- BUGS: -->
//bug upload antes de escolher destino -- ok
//bug upload de arquivo não existente -- ok
//bug mensagem para grupo que não existe -- ?? AlreadyClosedException
//bug adicionar usuario a um grupo que não existe -- ?? AlreadyClosedException
//bug adicionar usuario que não existe a um grupo -- ?? AlreadyClosedException

//AlreadyClosedException -> o canal fechar quando há um erro/exceção -> usar um shutDownListener para detectar quando o canal fecha?