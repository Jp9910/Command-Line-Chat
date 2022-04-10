package br.ufs.dcomp.ChatRabbitMQ;

import com.rabbitmq.client.*;
import java.io.IOException;

import java.util.Scanner;
import java.time.LocalDate;
import java.time.LocalTime;
import com.google.protobuf.ByteString;

public class Chat {

  private static Scanner sc = new Scanner(System.in);
  private static String prompt = new String("");
  private static String USUARIO = "";
  private static String pessoaDest = "";
  private static String grupoDest = "";
  private static boolean enviarMensagem = false;
  private static Channel channelT;
  private static Channel channelF;
  
  public static void main(String[] argv) throws Exception 
  {
    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost("54.210.34.218");
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
    //NOME_FILA_TEXTO = USUARIO+"-T";
    //NOME_FILA_ARQ = USUARIO+"-F";
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
        if(!grupo.isEmpty())
          msg_formatada = (  "("+data+" às "+hora+") "+emissor+"#"+grupo+" diz: "+texto   );
        else
          msg_formatada = (  "("+data+" às "+hora+") "+emissor+" diz: "+texto   );
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
        System.out.println("msg de arquivo");
      }
    };
    
    ChatThread threadT = new ChatThread("threadT", channelT, consumerT, USUARIO);
    ChatThread threadF = new ChatThread("threadF", channelF, consumerF, USUARIO);
    threadT.start();
    threadF.start();
    
    Chat.imprimirAjuda();
    Chat.imprimirComandos();
    prompt = ">> ";
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
                pessoaDest = operacao.substring(1);
                grupoDest = "";
                prompt = '@'+pessoaDest+">> ";
                System.out.println("Enviando mensagens para "+pessoaDest+".");
                enviarMensagem = true;
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
                        channelT.queueBind(USUARIO+"-T", nomeDoGrupo, "");
                        channelF.queueBind(USUARIO+"-F", nomeDoGrupo, "");
                    }
                }
                else if(comando.equals("addUser")) //adicionar usuário ao grupo
                {
                    if(argumentos.length == 3)
                    {
                        String novoIntegrante = argumentos[1];
                        String nomeDoGrupo = argumentos[2];
                        channelT.queueBind(novoIntegrante+"-T", nomeDoGrupo, "");
                        channelF.queueBind(novoIntegrante+"-F", nomeDoGrupo, "");
                    }
                    else {System.out.println("Comando inválido.");}
                }
                else if(comando.equals("delFromGroup")) //deletar usuário do grupo
                {
                    if(argumentos.length == 3)
                    {
                      String removerIntegrante = argumentos[1];
                      String nomeDoGrupo = argumentos[2];
                      channelT.queueUnbind(removerIntegrante+"-T", nomeDoGrupo, "");
                      channelF.queueUnbind(removerIntegrante+"-F", nomeDoGrupo, "");
                    }
                    else {System.out.println("Comando inválido.");}
                }
                else if(comando.equals("removeGroup")) //deletar grupo
                {
                    if(argumentos.length == 2)
                        channelT.exchangeDelete(argumentos[1]);
                }
                else {System.out.println("Comando inválido.");}
                break;
          
          case '#': //operação tipo trocar para mensagem para grupo
                grupoDest = operacao.substring(1);
                pessoaDest = "";
                prompt = '#'+grupoDest+">> ";
                System.out.println("Enviando mensagens para o grupo "+grupoDest+".");
                enviarMensagem = true;
                break;
          
          default: //operação tipo enviar mensagem
                /*
                * Montar o protocol buffer. O arquivo "MensagemProto.java" contém todas as 'messages' (classe Mensagem e classe Conteúdo).
                * Builders têm getters e setters. Servem para preencher o objeto. Usar .build() para transformar em uma Message.
                * Messages têm apenas getters e são imutáveis.
                */
                if(enviarMensagem)
                {
                  boolean isGroupMessage = pessoaDest.isEmpty();
                  MensagemProto.Mensagem mensagem_construida;
                  if(isGroupMessage)
                    mensagem_construida = Chat.MontarMensagemParaGrupo(operacao,USUARIO,grupoDest);
                  else
                    mensagem_construida = Chat.MontarMensagemParaUsuario(operacao,USUARIO);
                  
                  //Serializar a Mensagem em um vetor de bytes para depois enviar
                  byte[] mensagem_serializada = mensagem_construida.toByteArray();
                  
                  if(isGroupMessage)
                    channelT.basicPublish(grupoDest+"-T","",null,mensagem_serializada); //mensagem para grupo
                  else
                    channelT.basicPublish("",pessoaDest+"-T",null,mensagem_serializada); //mensagem para pessoa
                  
                }
                else{System.out.println("Operação inválida.");}
        }
    }
  }
  
  /*
  * Função auxiliar para montar uma mensagem que será enviada para um usuário usando protocol buffers
  */
  public static MensagemProto.Mensagem MontarMensagemParaUsuario(String msg, String emissor)
  {
    //Montar o conteúdo da mensagem (classe Conteúdo)
    MensagemProto.Conteudo.Builder conteudo = MensagemProto.Conteudo.newBuilder();
    conteudo.setTipo("text/plain")
            .setCorpo(ByteString.copyFromUtf8(msg));
    
    String data = Chat.getData();
    String hora = Chat.getHora();
    
    //Montar a mensagem (classe Mensagem), definindo o conteúdo para o montado acima.
    MensagemProto.Mensagem.Builder mensagem = MensagemProto.Mensagem.newBuilder();
    MensagemProto.Mensagem mensagem_construida = 
      mensagem.setEmissor(emissor)
              .setData(data)
              .setHora(hora)
              .setConteudo(conteudo)
              .build();
    
    return mensagem_construida;
  }
  
  /*
  * Função auxiliar para montar uma mensagem que será enviada para grupo usando protocol buffers
  */
  public static MensagemProto.Mensagem MontarMensagemParaGrupo(String msg, String emissor, String grupoDest)
  {
    //Montar o conteúdo da mensagem (classe Conteúdo)
    MensagemProto.Conteudo.Builder conteudo = MensagemProto.Conteudo.newBuilder();
    conteudo.setTipo("text/plain")
            .setCorpo(ByteString.copyFromUtf8(msg));
    
    String data = Chat.getData();
    String hora = Chat.getHora();
    
    //Montar a mensagem (classe Mensagem), definindo o conteúdo para o montado acima.
    MensagemProto.Mensagem.Builder mensagem = MensagemProto.Mensagem.newBuilder();
    MensagemProto.Mensagem mensagem_construida = 
      mensagem.setEmissor(emissor)
              .setData(data)
              .setHora(hora)
              .setGrupo(grupoDest)
              .setConteudo(conteudo)
              .build();
    
    return mensagem_construida;
  }
  
  /*
  * Função auxiliar para retornar a hora atual já formatada para a troca de mensagens
  */
  public static String getHora()
  {
    LocalTime hora = java.time.LocalTime.now();
    String hora_formatada = String.valueOf(hora.getHour()) +":"+ String.valueOf(hora.getMinute());
    
    return hora_formatada;
  }
  
  /*
  * Função auxiliar para retornar a data atual já formatada para a troca de mensagens
  */
  public static String getData()
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
  public static void imprimirAjuda()
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
  public static void imprimirComandos()
  {
    System.out.println("/------------------------------------------------LISTA DE COMANDOS-------------------------------------------------\\");
    System.out.println("| Digite '!addGroup <nome_do_grupo>' para criar um novo grupo. Ex: !addGroup amigos                                |");
    System.out.println("| Digite '!addUser <usuario> <nome_do_grupo>' para adicionar um usuário a um grupo. Ex: !addUser joao amigos       |");
    System.out.println("| Digite '!delFromGroup <usuário> <nome_do_grupo>' para remover um usuário do grupo. Ex: !delFromGroup joao amigos |");
    System.out.println("| Digite '!removeGroup <nome_do_grupo>' para deletar um grupo. Ex: !removeGroup amigos                             |");
    System.out.println("\\------------------------------------------------------------------------------------------------------------------/");
  }
}

//Dúvidas
//uma pessoa pode enviar mensagem para um grupo que ela não faz parte?
//quem envia mensagem para o grupo também recebe a própria mensagem que enviou?
//
//
//To do:
//criar threads para o envio
//criar um canal para texto e um canal para arquivos (pode usar o mesmo canal para consumir e para enviar - 1 canal para consumir e enviar texto, e 1 canal para consumir e enviar arquivos)
//criar fila para textos e fila para arquivos
//criar exchange do tipo Topic. Esse exchange usa uma tag para escolher a fila para que vai mandar a mensagem (que pode ser texto ou arquivo)