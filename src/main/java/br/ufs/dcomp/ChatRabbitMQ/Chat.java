package br.ufs.dcomp.ChatRabbitMQ;

import com.rabbitmq.client.*;
import java.io.IOException;
import java.util.Scanner;
import java.util.Arrays;
import java.io.File;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.nio.file.*;
import com.google.protobuf.ByteString;
// <!-- request http -->
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class Chat {

  private static Scanner sc = new Scanner(System.in);
  private static String prompt = new String("");
  private static String USUARIO = "";
  private static String pessoaDest = "";
  private static String grupoDest = "";
  private static boolean escolheuDestinatario = false;
  private static Channel channelT;
  private static Channel channelF;
  private static String login;
  private static String authHeaderName;
  private static String authHeaderValue;
  private static final String restResource = "http://RabbitMQ-LB-ef9f61bc629530a1.elb.us-east-1.amazonaws.com";
  
  public static void main(String[] argv) throws Exception 
  {
    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost("RabbitMQ-LB-ef9f61bc629530a1.elb.us-east-1.amazonaws.com");
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
    
    //declarar comportamento da fun????o de recebimento da fila de textos
    Consumer consumerT = new DefaultConsumer(channelT) 
    {
      public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException 
      {
          // O que o receptor far?? quando chegar uma mensagem. "body" ?? o conte??do da mensagem (representado em bytes)
          // Desserializar o conte??do recebido
          MensagemProto.Mensagem mensagem_recebida = MensagemProto.Mensagem.parseFrom(body);
          String emissor = mensagem_recebida.getEmissor();
          String data = mensagem_recebida.getData();
          String hora = mensagem_recebida.getHora();
          String grupo = mensagem_recebida.getGrupo();
          
          MensagemProto.Conteudo conteudo = mensagem_recebida.getConteudo();
          String texto = conteudo.getCorpo().toStringUtf8();
          
          // Formato da mensagem para usu??rio: (21/09/2016 ??s 20:53) marciocosta diz: E a??, Tarcisio! Vamos sim!
          // Formato da mensagem para grupo:   (21/09/2018 ??s 21:50) joaosantos#amigos diz: Ol??, amigos!!!
          String msg_formatada = "";
          if(grupo.isEmpty())
              msg_formatada = (  "("+data+" ??s "+hora+") "+emissor+" diz: "+texto   );
          else
              msg_formatada = (  "("+data+" ??s "+hora+") "+emissor+"#"+grupo+" diz: "+texto   );
          System.out.println("\n"+msg_formatada);
          System.out.print(prompt);
      }
    };
    
    //declarar comportamento da fun????o de recebimento da fila de arquivos
    Consumer consumerF = new DefaultConsumer(channelF) 
    {
        public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException 
        {
            // O que o receptor far?? quando chegar uma mensagem. "body" ?? o conte??do da mensagem (representado em bytes)
            
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
                    System.out.println("Arquivo j?? existe. Escrevendo conte??do recebido nele.");
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
                msg_formatada = (  "("+data+" ??s "+hora+") Arquivo \""+nomeArquivoRecebido+"\" recebido de @"+emissor   );
            else
                msg_formatada = (  "("+data+" ??s "+hora+") Arquivo \""+nomeArquivoRecebido+"\" recebido de @"+emissor+"#"+grupo   );
            System.out.println(msg_formatada);
            System.out.print(prompt);
        }
    };
    
    ThreadRecebimento threadT = new ThreadRecebimento("threadT", channelT, consumerT, USUARIO, "-T");
    ThreadRecebimento threadF = new ThreadRecebimento("threadF", channelF, consumerF, USUARIO, "-F");
    threadT.start();
    threadF.start();

    login = "jp" + ":" + "9910";
    authHeaderName = "Authorization";
    authHeaderValue = "Basic " + java.util.Base64.getEncoder().encodeToString(login.getBytes());

    Chat_Utils.imprimirAjuda();
    Chat_Utils.imprimirComandos();
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
      if(!operacao.isEmpty()) //se a opera????o n??o for vazia
      {
          char primeirochar = operacao.charAt(0);
          switch (primeirochar)
          {
            case '@': //opera????o tipo trocar para mensagem para pessoa
                  if(operacao.substring(1).equals(USUARIO)){
                    System.out.println("N??o ?? poss??vel enviar mensagens para s?? mesmo.");
                    break;
                  }
                  pessoaDest = operacao.substring(1);
                  grupoDest = "";
                  prompt = "["+USUARIO+"] @"+pessoaDest+">> ";
                  System.out.println("Enviando mensagens para "+pessoaDest+".");
                  escolheuDestinatario = true;
                  break;
            
            case '!': //opera????o tipo comando
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
                          System.out.println("Comando inv??lido.");
                  }
                  else if(comando.equals("addUser")) //adicionar usu??rio ao grupo
                  {
                      if(argumentos.length == 3)
                      {
                          String novoIntegrante = argumentos[1];
                          String nomeDoGrupo = argumentos[2];
                          try {
                              channelT.queueBind(novoIntegrante+"-T", nomeDoGrupo, "-T");
                              channelF.queueBind(novoIntegrante+"-F", nomeDoGrupo, "-F");
                          } catch (IOException ioe) {
                              System.out.print("Grupo ou usu??rio n??o existe.");
                              ioe.printStackTrace();
                              System.out.print(prompt);
                          } catch (AlreadyClosedException ace) {
                              System.out.println(" !! Erro no canal. Reinicie o chat !! ");
                              ace.printStackTrace();
                              System.out.print(prompt);
                          }
                      }
                      else {System.out.println("Comando inv??lido.");}
                  }
                  else if(comando.equals("delFromGroup")) //deletar usu??rio do grupo
                  {
                      if(argumentos.length == 3)
                      {
                          String removerIntegrante = argumentos[1];
                          String nomeDoGrupo = argumentos[2];
                          try {
                              channelT.queueUnbind(removerIntegrante+"-T", nomeDoGrupo, "-T");
                              channelF.queueUnbind(removerIntegrante+"-F", nomeDoGrupo, "-F");
                          } catch (IOException ioe) {
                              System.out.print("Grupo ou usu??rio n??o existe.");
                              ioe.printStackTrace();
                              System.out.print(prompt);
                          } catch (AlreadyClosedException ace) {
                              System.out.println(" !! Erro no canal. Reinicie o chat !! ");
                              ace.printStackTrace();
                              System.out.print(prompt);
                          }
                      }
                      else
                          System.out.println("Comando inv??lido.");
                  }
                  else if(comando.equals("removeGroup")) //deletar grupo
                  {
                      if(argumentos.length == 2)
                          channelT.exchangeDelete(argumentos[1]);
                      else
                          System.out.println("Comando inv??lido.");
                  }
                  else if(comando.equals("upload")) // upload de arquivo
                  {
                      if(argumentos.length == 2)
                          if(escolheuDestinatario)
                              Chat.enviarArquivo(argumentos[1]);
                          else
                              System.out.println("Escolha o grupo ou usu??rio destinat??rio.");
                      else
                          System.out.println("Comando inv??lido.");
                  }
                  else if(comando.equals("listUsers")) //listar usu??rios de um dado grupo
                  {
                      if(argumentos.length == 2)
                          listarUsuariosDoGrupo(argumentos[1]);
                      else
                          System.out.println("Comando inv??lido.");
                  }
                  else if(comando.equals("listGroups")) //listar os grupos dos quais o usu??rio atual faz parte
                  {
                      if(argumentos.length == 1)
                          listarGruposDoUsuario(USUARIO);
                      else
                          System.out.println("Comando inv??lido.");
                  }
                  else
                      System.out.println("Comando inv??lido.");
                  break;
            
            case '#': //opera????o tipo trocar para mensagem para grupo
                  grupoDest = operacao.substring(1);
                  pessoaDest = "";
                  prompt = "["+USUARIO+"] #"+grupoDest+">> ";
                  System.out.println("Enviando mensagens para o grupo "+grupoDest+".");
                  escolheuDestinatario = true;
                  break;
            
            default: //opera????o tipo enviar mensagem de texto
                  /*
                  * Montar o protocol buffer. O arquivo "MensagemProto.java" cont??m todas as 'messages' (classe Mensagem e classe Conte??do).
                  * Builders t??m getters e setters. Servem para preencher o objeto. Usar .build() para transformar em uma Message.
                  * Messages t??m apenas getters e s??o imut??veis.
                  */
                  if(escolheuDestinatario)
                  {
                    Chat.enviarTexto(operacao);
                  }
                  else{System.out.println("Opera????o inv??lida.");}
          }
      }
  }
  
  /*
  * Fun????o para criar uma thread que far?? o envio de arquivo como mensagem
  */
  private static void enviarArquivo(String caminho)
  {
      ThreadEnvio thEnvio = new ThreadEnvio("thEnvio",channelF,USUARIO,pessoaDest,grupoDest,"-F",caminho);
      thEnvio.start();
  }
  
  /*
  * Fun????o para enviar texto como mensagem
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
          System.out.println(" !! Erro no publish. N??o foi poss??vel enviar a mensagem. Tente Novamente. !! ");
          ioe.printStackTrace();
          System.out.print(prompt);
      } catch (AlreadyClosedException ace){
          System.out.println(" !! Erro no canal. Reinicie o chat !! ");
          ace.printStackTrace();
          System.out.print(prompt);
      }
  }
  
  /*
  * Fun????o para listar todos os usu??rios de um dado grupo
  */
  public static void listarUsuariosDoGrupo(String grupo)
  {
      Gson gson = new Gson();
      Client client = ClientBuilder.newClient();
      Response resposta = client.target( restResource )
          .path("/api/exchanges/%2f/"+grupo+"/bindings/source") // lista todos os binds que tem o exchange grupo como source
      	  .request(MediaType.APPLICATION_JSON)
          .header( authHeaderName, authHeaderValue )
          .get();
      
      if (resposta.getStatus() == 200) {
        String json = resposta.readEntity(String.class);
        Usuario[] usuarios = gson.fromJson(json, Usuario[].class);
        if(usuarios.length > 0 && usuarios[0].get().endsWith("-F"))
            System.out.print(usuarios[0].get().substring(0,usuarios[0].get().length()-2));
        for (int i = 1; i < usuarios.length; i++){
            if(usuarios[i].get().endsWith("-F"))
              System.out.print(", "+usuarios[i].get().substring(0,usuarios[i].get().length()-2));
        }
        System.out.println();
      } 
      else System.out.println("Erro "+resposta.getStatus());
  }
  
  /*
  * Fun????o para listar todos os grupos de um dado usu??rio
  */
  public static void listarGruposDoUsuario(String usuario)
  {
      Client client = ClientBuilder.newClient();
      Response resposta = client.target( restResource )
          .path("/api/queues/%2f/"+usuario+"-T/bindings") //
      	  .request(MediaType.APPLICATION_JSON)
          .header( authHeaderName, authHeaderValue )
          .get();
      if (resposta.getStatus() == 200) {
        Gson gson = new Gson();
      	String json = resposta.readEntity(String.class);
      	Grupo[] grupos = gson.fromJson(json, Grupo[].class);
        for (int i = 0; i < grupos.length-1; i++) {
            if(!(grupos[i].getSource().isEmpty()))
              System.out.print(grupos[i].getSource()+", ");
        }
        System.out.println(grupos[grupos.length-1].getSource());
      } else {
        System.out.println("Erro "+resposta.getStatus());
      }
  }
  
  public static void listarExchanges()
  {
      Client client = ClientBuilder.newClient();
      Response resposta = client.target( restResource )
        .path("/api/exchanges") //
    	  .request(MediaType.APPLICATION_JSON)
        .header( authHeaderName, authHeaderValue )
        .get();
    
      if (resposta.getStatus() == 200) {
        //return resposta;
      	String json = resposta.readEntity(String.class);
      	GsonBuilder builder = new GsonBuilder();
        builder.setPrettyPrinting();
        Gson gson = builder.create();
        Grupo[] exchanges = gson.fromJson(json, Grupo[].class);
        System.out.print("Exchanges: ");
        for (int i = 0; i < exchanges.length; i++)
            System.out.print(exchanges[i].getName()+", ");
        System.out.println();
      } else {
        System.out.println(resposta.getStatus());
      }
  }
  
  /*
  * Fun????o auxiliar para montar uma mensagem de texto que ser?? enviada para um usu??rio usando protocol buffers
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
          System.out.println(" !! Erro ao carregar arquivo. Verifique se o caminho est?? correto. !! ");
          //ioe.printStackTrace();
          System.out.print(prompt);
          return null;
      }
    
      //Montar o conte??do da mensagem (classe Conte??do)
      MensagemProto.Conteudo.Builder conteudo = MensagemProto.Conteudo.newBuilder();
      conteudo.setTipo(tipoMime)
              .setCorpo(ByteString.copyFrom(conteudoArquivo))
              .setNome(nomeArquivo);
      
      String data = Chat_Utils.getData();
      String hora = Chat_Utils.getHora();
      
      //Montar a mensagem (classe Mensagem), definindo o conte??do para o montado acima.
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
  * Fun????o auxiliar para montar uma mensagem de texto que ser?? enviada usando protocol buffers
  */
  public static MensagemProto.Mensagem MontarMensagemTexto(String texto, String emissor, String grupoDest)
  {
      //Montar o conte??do da mensagem (classe Conte??do)
      MensagemProto.Conteudo.Builder conteudo = MensagemProto.Conteudo.newBuilder();
      conteudo.setTipo("text/plain")
              .setCorpo(ByteString.copyFromUtf8(texto));
      
      String data = Chat_Utils.getData();
      String hora = Chat_Utils.getHora();
      
      //Montar a mensagem (classe Mensagem), definindo o conte??do para o montado acima.
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
}
//!upload /home/ubuntu/environment/chat-em-linha-de-comando-via-rabbitmq-Jp9910/ArquivosParaEnviar/arquivoteste.txt
//!upload /home/ubuntu/environment/chat-em-linha-de-comando-via-rabbitmq-Jp9910/ArquivosParaEnviar/livro.pdf

//To do:
//criar threads para o recebimento -- ok
//criar threads para o envio -- ok
//criar um canal para texto e um canal para arquivos -- ok (pode usar o mesmo canal para consumir e para enviar - 1 canal para consumir e enviar texto, e 1 canal para consumir e enviar arquivos)
//criar fila para textos e fila para arquivos -- ok
//criar exchange do tipo Topic. -- ok (Esse exchange usa uma routingKey para escolher a fila para que vai mandar a mensagem (que pode ser texto ou arquivo))

// <!-- BUGS: -->
//bug upload antes de escolher destino -- ok
//bug upload de arquivo n??o existente -- ok
//bug mensagem para grupo que n??o existe -- ?? AlreadyClosedException
//bug adicionar usuario a um grupo que n??o existe -- ?? AlreadyClosedException
//bug adicionar usuario que n??o existe a um grupo -- ?? AlreadyClosedException

//AlreadyClosedException -> o canal fechar quando h?? um erro/exce????o -> usar um shutDownListener para detectar quando o canal fecha?