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
  private static Channel channel;
  
  public static void main(String[] argv) throws Exception 
  {
    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost("52.91.160.18");
    factory.setUsername("jp");
    factory.setPassword("9910");
    factory.setVirtualHost("/");
    Connection connection = factory.newConnection();
    channel = connection.createChannel();
    
    while(USUARIO.isEmpty())
    {
      System.out.print("User: ");
      USUARIO = sc.nextLine();
    }
                      //(queue-name, durable, exclusive, auto-delete, params); 
    channel.queueDeclare(USUARIO, false,   false,     false,       null);
    
    Consumer consumer = new DefaultConsumer(channel) 
    {
      public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException 
      {
        // O que o receptor fará quando chegar uma mensagem. "body" é o conteúdo da mensagem (representado em bytes)
        // Desserializar o conteúdo recebido
        MensagemProto.Mensagem mensagem_recebida = MensagemProto.Mensagem.parseFrom(body);
        String emissor = mensagem_recebida.getEmissor();
        String data = mensagem_recebida.getData();
        String hora = mensagem_recebida.getHora();
        
        MensagemProto.Conteudo conteudo = mensagem_recebida.getConteudo();
        String texto = conteudo.getCorpo().toStringUtf8();
        
        // Formato da mensagem: (21/09/2016 às 20:53) marciocosta diz: E aí, Tarcisio! Vamos sim!
        String msg_formatada = (  "("+data+" às "+hora+") "+emissor+" diz: "+texto   );
        System.out.println("\n"+msg_formatada);
        System.out.print(prompt);
      }
    };
                      //(queue-name, autoAck, consumer);    
    channel.basicConsume(USUARIO, true,    consumer);
    
    Chat.imprimirAjuda();
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
                if(comando.equals("addGroup")) //criar novo grupo (exchange do tipo fanout)
                {
                    if(argumentos.length == 2)
                    {
                        String nomeDoGrupo = argumentos[1];
                        channel.exchangeDeclare(nomeDoGrupo,"fanout");
                        channel.queueBind(USUARIO, nomeDoGrupo, "");
                    }
                }
                else if(comando.equals("addUser")) //adicionar usuário ao grupo
                {
                    if(argumentos.length == 3)
                    {
                        String novoIntegrante = argumentos[1];
                        String nomeDoGrupo = argumentos[2];
                        channel.queueBind(novoIntegrante, nomeDoGrupo, "");
                    }
                    else {System.out.println("Comando inválido.");}
                }
                else if(comando.equals("delFromGroup")) //deletar usuário do grupo
                {
                    if(argumentos.length == 3)
                    {
                      String removerIntegrante = argumentos[1];
                      String nomeDoGrupo = argumentos[2];
                      channel.queueUnbind(removerIntegrante, nomeDoGrupo, "");
                    }
                    else {System.out.println("Comando inválido.");}
                }
                else if(comando.equals("removeGroup")) //deletar grupo
                {
                    if(argumentos.length == 2)
                        channel.exchangeDelete(argumentos[1]);
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
                  MensagemProto.Mensagem mensagem_construida = Chat.montarMensagem(operacao,USUARIO);
                  
                  //Serializar a Mensagem em um vetor de bytes para depois enviar
                  byte[] mensagem_serializada = mensagem_construida.toByteArray();
                  
                  if(!pessoaDest.isEmpty())                                           //mensagem para pessoa
                    channel.basicPublish("",pessoaDest,null,mensagem_serializada);
                  else                                                                //mensagem para grupo
                    channel.basicPublish(grupoDest,"",null,mensagem_serializada);
                  
                }
                else{System.out.println("Operação inválida.");}
        }
    }
  }
  
  public static MensagemProto.Mensagem montarMensagem(String msg, String emissor)
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
  * Retorna a hora atual já formatada para a troca de mensagens
  */
  public static String getHora()
  {
    LocalTime hora = java.time.LocalTime.now();
    String hora_formatada = String.valueOf(hora.getHour()) +":"+ String.valueOf(hora.getMinute());
    
    return hora_formatada;
  }
  
  /*
  * Retorna a data atual já formatada para a troca de mensagens
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
  
  public static void imprimirAjuda()
  {
    System.out.println("/---------------------------------------------------------------------------\\");
    System.out.println("| Digite '@<destinatário>' para enviar mensagem para uma pessoa. Ex: @joao  |");
    System.out.println("| Digite '!<comando>' para executar um comando. Ex: !addGroup amigos       |");
    System.out.println("| Digite '#<grupo>' para enviar mensagem para um grupo. Ex: #amigos         |");
    System.out.println("\\---------------------------------------------------------------------------/");
  }
  
  public static void imprimirComandos()
  {
    System.out.println("/-----------------------------------------------------------------------------------------------------------------\\");
    System.out.println("| Digite '!addGroup <nome_do_grupo>' para criar um novo grupo. Ex: !addGroup amigos                                |");
    System.out.println("| Digite '!addUser <usuario> <nome_do_grupo>' para adicionar um usuário a um grupo. Ex: !addUser joao amigos       |");
    System.out.println("| Digite '!delFromGroup <usuário> <nome_do_grupo>' para remover um usuário do grupo. Ex: !delFromGroup joao amigos |");
    System.out.println("| Digite '!removeGroup <nome_do_grupo>' para deletar um grupo. Ex: !removeGroup amigos                             |");
    System.out.println("\\-----------------------------------------------------------------------------------------------------------------/");
  }
}
